package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.request.EncerramentoInternacaoRequest;
import ipiranga.fatec.qrvet.dtos.request.InternacaoRequest;
import ipiranga.fatec.qrvet.dtos.request.JejumRequest;
import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.dtos.response.*;
import ipiranga.fatec.qrvet.exceptions.InvalidRequestException;
import ipiranga.fatec.qrvet.exceptions.OperationConflictException;
import ipiranga.fatec.qrvet.exceptions.ResourceNotFoundException;
import ipiranga.fatec.qrvet.models.Baia;
import ipiranga.fatec.qrvet.models.Internacao;
import ipiranga.fatec.qrvet.models.Jejum;
import ipiranga.fatec.qrvet.models.Paciente;
import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.models.enums.InternacaoStatus;
import ipiranga.fatec.qrvet.repositories.*;
import ipiranga.fatec.qrvet.utils.PaginationUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InternacaoService {
    private final InternacaoRepository internacaoRepository;
    private final PacienteRepository pacienteRepository;
    private final BaiaService baiaService;
    private final UserRepository userRepository;
    private final JejumRepository jejumRepository;
    private final QrCodeService qrCodeService;

    public InternacaoService(
            InternacaoRepository internacaoRepository,
            PacienteRepository pacienteRepository,
            BaiaService baiaService,
            UserRepository userRepository,
            QrCodeService qrCodeService,
            JejumRepository jejumRepository) {
        this.internacaoRepository = internacaoRepository;
        this.pacienteRepository = pacienteRepository;
        this.baiaService = baiaService;
        this.userRepository = userRepository;
        this.qrCodeService = qrCodeService;
        this.jejumRepository = jejumRepository;
    }

    @Transactional
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public InternacaoResponse open(InternacaoRequest request) {
        Paciente paciente = pacienteRepository.findByIdForUpdate(request.pacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found."));
        if (internacaoRepository.existsByPacienteIdAndStatus(paciente.getId(), InternacaoStatus.ATIVA)) {
            throw new OperationConflictException("The patient already has an active hospitalization.");
        }

        Baia baia = baiaService.occupyAutomatically(request.baiaId());

        User veterinario = userRepository.findByIdAndRoleAndActiveTrue(
                        request.veterinarioId(), ipiranga.fatec.qrvet.models.enums.Role.VETERINARIO)
                .orElseThrow(() -> new ResourceNotFoundException("Active veterinarian not found."));

        String observacoes = "";
        if (request.observacoes() != null && !request.observacoes().isBlank()) {
            observacoes = request.observacoes().trim();
        }
        Internacao internacao = Internacao.builder()
                .paciente(paciente)
                .baia(baia)
                .veterinario(veterinario)
                .motivo(request.motivo().trim())
                .diagnosticoInicial(request.diagnosticoInicial().trim())
                .observacoes(observacoes)
                .build();
        return InternacaoResponse.from(internacaoRepository.saveAndFlush(internacao));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public PageResponse<InternacaoResponse> listActive(PaginationRequest pagination) {
        return PageResponse.from(internacaoRepository.findAllByStatus(
                InternacaoStatus.ATIVA, PaginationUtils.byId(pagination))
                .map(InternacaoResponse::from));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public InternacaoResponse findById(Long id) {
        return InternacaoResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public InternacaoQrCodeResponse qrCode(Long id) {
        Internacao internacao = find(id);
        String url = qrCodeService.url(internacao.getUuidToken());
        return new InternacaoQrCodeResponse(internacao.getUuidToken().toString(), url, qrCodeService.base64(url));
    }

    @Transactional(readOnly = true)
    public InternacaoPublicResponse publicDetails(java.util.UUID uuidToken) {
        Internacao internacao = internacaoRepository.findByUuidToken(uuidToken)
                .orElseThrow(() -> new ResourceNotFoundException("Hospitalization not found."));
        boolean jejumAtivo = jejumRepository.existsByInternacaoIdAndAtivoTrue(internacao.getId());
        return InternacaoPublicResponse.from(internacao, jejumAtivo);
    }

    @Transactional
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public InternacaoResponse close(Long id, EncerramentoInternacaoRequest request) {
        Internacao internacao = internacaoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospitalization not found."));
        if (internacao.getStatus() != InternacaoStatus.ATIVA) {
            throw new OperationConflictException("The hospitalization has already been closed.");
        }
        try {
            internacao.close(InternacaoStatus.valueOf(request.statusEncerramento().name()));
            baiaService.releaseAutomatically(internacao.getBaia().getId());
            jejumRepository.findActiveForUpdate(internacao.getId()).ifPresent(Jejum::end);
        } catch (IllegalStateException exception) {
            throw new OperationConflictException(exception.getMessage());
        }
        return InternacaoResponse.from(internacao);
    }

    @Transactional
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO')")
    public JejumResponse startFasting(Long internacaoId, JejumRequest request) {
        Internacao internacao = findForUpdate(internacaoId);
        ensureActive(internacao);
        if (jejumRepository.existsByInternacaoIdAndAtivoTrue(internacaoId)) {
            throw new OperationConflictException("The hospitalization already has an active fasting period.");
        }

        Jejum jejum = Jejum.builder()
                .internacao(internacao)
                .motivo(request.motivo().trim())
                .build();
        return JejumResponse.from(jejumRepository.saveAndFlush(jejum));
    }

    @Transactional
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO')")
    public JejumResponse endFasting(Long internacaoId) {
        Internacao internacao = findForUpdate(internacaoId);
        ensureActive(internacao);
        Jejum jejum = jejumRepository.findActiveForUpdate(internacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Active fasting period not found."));
        jejum.end();
        return JejumResponse.from(jejum);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO')")
    public List<JejumResponse> fastingHistory(Long internacaoId) {
        Internacao internacao = find(internacaoId);
        return jejumRepository.findAllByInternacaoIdOrderByDataHoraInicioDesc(internacao.getId())
                .stream()
                .map(JejumResponse::from)
                .toList();
    }

    private Internacao find(Long id) {
        return internacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospitalization not found."));
    }

    private Internacao findForUpdate(Long id) {
        return internacaoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospitalization not found."));
    }

    private void ensureActive(Internacao internacao) {
        if (internacao.getStatus() != InternacaoStatus.ATIVA) {
            throw new InvalidRequestException("Fasting can only be changed for an active hospitalization.");
        }
    }
}
