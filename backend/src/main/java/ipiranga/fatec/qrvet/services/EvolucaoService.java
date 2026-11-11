package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.request.EvolucaoRequest;
import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.dtos.response.EvolucaoResponse;
import ipiranga.fatec.qrvet.dtos.response.PageResponse;
import ipiranga.fatec.qrvet.exceptions.InvalidRequestException;
import ipiranga.fatec.qrvet.exceptions.ResourceNotFoundException;
import ipiranga.fatec.qrvet.models.Evolucao;
import ipiranga.fatec.qrvet.models.enums.*;
import ipiranga.fatec.qrvet.repositories.EvolucaoRepository;
import ipiranga.fatec.qrvet.repositories.InternacaoRepository;
import ipiranga.fatec.qrvet.security.CurrentUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
@PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO')")
public class EvolucaoService {
    private final EvolucaoRepository evolucoes;
    private final InternacaoRepository internacoes;
    private final CurrentUser currentUser;
    private final AuditService audit;

    public EvolucaoService(EvolucaoRepository evolucoes, InternacaoRepository internacoes, CurrentUser currentUser, AuditService audit) {
        this.evolucoes = evolucoes;
        this.internacoes = internacoes;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    @Transactional
    public EvolucaoResponse create(Long internacaoId, EvolucaoRequest request) {
        var internacao = internacoes.findByIdForUpdate(internacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospitalization not found."));
        if (internacao.getStatus() != InternacaoStatus.ATIVA)
            throw new InvalidRequestException("Só é possível adicionar evolução a uma internação ativa.");
        var response = EvolucaoResponse.from(evolucoes.saveAndFlush(new Evolucao(internacao, currentUser.getCurrent(), request.texto().strip())));
        audit.log(AuditAction.EVOLUTION_CREATED, AuditResource.HOSPITALIZATION, String.valueOf(internacaoId),
                Map.of("evolutionId", response.id()));
        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<EvolucaoResponse> list(Long internacaoId, PaginationRequest pagination) {
        if (!internacoes.existsById(internacaoId)) throw new ResourceNotFoundException("Hospitalization not found.");
        var pageable = PageRequest.of(pagination.page(), pagination.size(), Sort.by(Sort.Direction.DESC, "dataHoraRegistro", "id"));
        return PageResponse.from(evolucoes.findAllByInternacaoId(internacaoId, pageable).map(EvolucaoResponse::from));
    }
}
