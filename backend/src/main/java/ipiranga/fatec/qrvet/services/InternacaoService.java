package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.request.EncerramentoInternacaoRequest;
import ipiranga.fatec.qrvet.dtos.request.InternacaoRequest;
import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.dtos.response.*;
import ipiranga.fatec.qrvet.exceptions.InvalidRequestException;
import ipiranga.fatec.qrvet.exceptions.OperationConflictException;
import ipiranga.fatec.qrvet.exceptions.ResourceNotFoundException;
import ipiranga.fatec.qrvet.models.Baia;
import ipiranga.fatec.qrvet.models.Internacao;
import ipiranga.fatec.qrvet.models.Paciente;
import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.models.enums.BaiaStatus;
import ipiranga.fatec.qrvet.models.enums.InternacaoStatus;
import ipiranga.fatec.qrvet.repositories.*;
import ipiranga.fatec.qrvet.utils.PaginationUtils;
import ipiranga.fatec.qrvet.utils.QrCodeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InternacaoService {
    private final InternacaoRepository internacaoRepository;
    private final PacienteRepository pacienteRepository;
    private final BaiaRepository baiaRepository;
    private final UserRepository userRepository;
    private final JejumRepository jejumRepository;
    private final QrCodeService qrCodeService;

    public InternacaoService(
            InternacaoRepository internacaoRepository,
            PacienteRepository pacienteRepository,
            BaiaRepository baiaRepository,
            UserRepository userRepository,
            QrCodeService qrCodeService,
            JejumRepository jejumRepository) {
        this.internacaoRepository = internacaoRepository;
        this.pacienteRepository = pacienteRepository;
        this.baiaRepository = baiaRepository;
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
        Baia baia = baiaRepository.findByIdForUpdate(request.baiaId())
                .orElseThrow(() -> new ResourceNotFoundException("Bay not found."));
        if (baia.getStatus() != BaiaStatus.DISPONIVEL) {
            throw new InvalidRequestException("The bay is not available.");
        }

        User veterinario = userRepository.findByIdAndRoleAndActiveTrue(
                        request.veterinarioId(), ipiranga.fatec.qrvet.models.enums.Role.VETERINARIO)
                .orElseThrow(() -> new ResourceNotFoundException("Active veterinarian not found."));

        baia.occupyAutomatically();
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
        Baia baia = baiaRepository.findByIdForUpdate(internacao.getBaia().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bay not found."));
        try {
            internacao.close(InternacaoStatus.valueOf(request.statusEncerramento().name()));
            baia.releaseAutomatically();
        } catch (IllegalStateException exception) {
            throw new OperationConflictException(exception.getMessage());
        }
        return InternacaoResponse.from(internacao);
    }

    private Internacao find(Long id) {
        return internacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospitalization not found."));
    }
}
