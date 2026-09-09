package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.request.JejumRequest;
import ipiranga.fatec.qrvet.dtos.response.JejumResponse;
import ipiranga.fatec.qrvet.exceptions.InvalidRequestException;
import ipiranga.fatec.qrvet.exceptions.OperationConflictException;
import ipiranga.fatec.qrvet.exceptions.ResourceNotFoundException;
import ipiranga.fatec.qrvet.models.Internacao;
import ipiranga.fatec.qrvet.models.Jejum;
import ipiranga.fatec.qrvet.models.enums.InternacaoStatus;
import ipiranga.fatec.qrvet.repositories.InternacaoRepository;
import ipiranga.fatec.qrvet.repositories.JejumRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JejumService {
    private final InternacaoRepository internacaoRepository;
    private final JejumRepository jejumRepository;

    public JejumService(InternacaoRepository internacaoRepository, JejumRepository jejumRepository) {
        this.internacaoRepository = internacaoRepository;
        this.jejumRepository = jejumRepository;
    }

    @Transactional
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO')")
    public JejumResponse start(Long internacaoId, JejumRequest request) {
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
    public JejumResponse end(Long internacaoId) {
        Internacao internacao = findForUpdate(internacaoId);
        ensureActive(internacao);
        Jejum jejum = jejumRepository.findActiveForUpdate(internacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Active fasting period not found."));
        jejum.end();
        return JejumResponse.from(jejum);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO')")
    public List<JejumResponse> history(Long internacaoId) {
        Internacao internacao = internacaoRepository.findById(internacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospitalization not found."));
        return jejumRepository.findAllByInternacaoIdOrderByDataHoraInicioDesc(internacao.getId())
                .stream()
                .map(JejumResponse::from)
                .toList();
    }

    private Internacao findForUpdate(Long internacaoId) {
        return internacaoRepository.findByIdForUpdate(internacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospitalization not found."));
    }

    private void ensureActive(Internacao internacao) {
        if (internacao.getStatus() != InternacaoStatus.ATIVA) {
            throw new InvalidRequestException("Fasting can only be changed for an active hospitalization.");
        }
    }
}
