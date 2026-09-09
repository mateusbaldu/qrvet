package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.request.RegistroAlimentacaoRequest;
import ipiranga.fatec.qrvet.dtos.response.RegistroAlimentacaoResponse;
import ipiranga.fatec.qrvet.exceptions.InvalidRequestException;
import ipiranga.fatec.qrvet.exceptions.ResourceNotFoundException;
import ipiranga.fatec.qrvet.models.Internacao;
import ipiranga.fatec.qrvet.models.RegistroAlimentacao;
import ipiranga.fatec.qrvet.models.enums.InternacaoStatus;
import ipiranga.fatec.qrvet.repositories.InternacaoRepository;
import ipiranga.fatec.qrvet.repositories.JejumRepository;
import ipiranga.fatec.qrvet.repositories.RegistroAlimentacaoRepository;
import ipiranga.fatec.qrvet.security.CurrentUser;
import ipiranga.fatec.qrvet.specifications.RegistroAlimentacaoSpecifications;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistroAlimentacaoService {
    private final InternacaoRepository internacaoRepository;
    private final RegistroAlimentacaoRepository registroRepository;
    private final JejumRepository jejumRepository;
    private final CurrentUser currentUser;

    public RegistroAlimentacaoService(
            InternacaoRepository internacaoRepository,
            RegistroAlimentacaoRepository registroRepository,
            JejumRepository jejumRepository,
            CurrentUser currentUser) {
        this.internacaoRepository = internacaoRepository;
        this.registroRepository = registroRepository;
        this.jejumRepository = jejumRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO')")
    public RegistroAlimentacaoResponse create(Long internacaoId, RegistroAlimentacaoRequest request) {
        Internacao internacao = findForUpdate(internacaoId);
        ensureActive(internacao);
        if (jejumRepository.existsByInternacaoIdAndAtivoTrue(internacaoId)) {
            throw new InvalidRequestException("Food cannot be recorded while the hospitalization has active fasting.");
        }

        String observacao = null;
        if (request.aceitacaoObservacao() != null && !request.aceitacaoObservacao().isBlank()) {
            observacao = request.aceitacaoObservacao().trim();
        }

        RegistroAlimentacao registro = RegistroAlimentacao.builder()
                .internacao(internacao)
                .usuario(currentUser.getCurrent())
                .alimento(request.alimento().trim())
                .quantidade(request.quantidade().trim())
                .aceitacaoObservacao(observacao)
                .build();
        return RegistroAlimentacaoResponse.from(registroRepository.saveAndFlush(registro));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO')")
    public List<RegistroAlimentacaoResponse> history(Long internacaoId) {
        if (!internacaoRepository.existsById(internacaoId)) {
            throw new ResourceNotFoundException("Hospitalization not found.");
        }
        return registroRepository.findAll(
                        RegistroAlimentacaoSpecifications.byInternacaoId(internacaoId),
                        Sort.by(Sort.Direction.DESC, "dataHoraRegistro"))
                .stream()
                .map(RegistroAlimentacaoResponse::from)
                .toList();
    }

    private Internacao findForUpdate(Long internacaoId) {
        return internacaoRepository.findByIdForUpdate(internacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospitalization not found."));
    }

    private void ensureActive(Internacao internacao) {
        if (internacao.getStatus() != InternacaoStatus.ATIVA) {
            throw new InvalidRequestException("Food can only be recorded for an active hospitalization.");
        }
    }
}
