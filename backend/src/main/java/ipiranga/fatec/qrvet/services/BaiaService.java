package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.request.BaiaDetailsRequest;
import ipiranga.fatec.qrvet.dtos.request.BaiaRequest;
import ipiranga.fatec.qrvet.dtos.request.BaiaStatusRequest;
import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.dtos.response.BaiaResponse;
import ipiranga.fatec.qrvet.dtos.response.PageResponse;
import ipiranga.fatec.qrvet.exceptions.OperationConflictException;
import ipiranga.fatec.qrvet.exceptions.ResourceAlreadyExistsException;
import ipiranga.fatec.qrvet.exceptions.ResourceNotFoundException;
import ipiranga.fatec.qrvet.models.Baia;
import ipiranga.fatec.qrvet.models.enums.BaiaStatus;
import ipiranga.fatec.qrvet.repositories.BaiaRepository;
import ipiranga.fatec.qrvet.utils.PaginationUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BaiaService {
    private final BaiaRepository repository;

    public BaiaService(BaiaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN')")
    public BaiaResponse create(BaiaRequest request) {
        String identificacao = normalizedIdentification(request.identificacao());
        if (repository.existsByIdentificacaoIgnoreCase(identificacao)) {
            throw new ResourceAlreadyExistsException("A bay with this identification already exists.");
        }

        BaiaStatus status = request.status() == null ? BaiaStatus.DISPONIVEL : request.status();
        if (status == BaiaStatus.OCUPADA) {
            throw new OperationConflictException("A bay cannot be created as occupied.");
        }
        String observacao = null;
        if (request.observacao() != null && !request.observacao().isBlank()) {
            observacao = request.observacao().trim();
        }

        Baia baia = Baia.builder()
                .identificacao(identificacao)
                .status(status)
                .observacao(observacao)
                .build();
        try {
            return BaiaResponse.from(repository.saveAndFlush(baia));
        } catch (DataIntegrityViolationException exception) {
            throw new ResourceAlreadyExistsException("A bay with this identification already exists.");
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public PageResponse<BaiaResponse> list(PaginationRequest pagination, BaiaStatus status) {
        PageRequest pageable = PaginationUtils.byId(pagination, org.springframework.data.domain.Sort.Direction.ASC);
        if (status == null) {
            return PageResponse.from(repository.findAll(pageable).map(BaiaResponse::from));
        }
        return PageResponse.from(repository.findAllByStatus(status, pageable).map(BaiaResponse::from));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public BaiaResponse findById(Long id) {
        return BaiaResponse.from(find(id));
    }

    @Transactional
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN')")
    public BaiaResponse update(Long id, BaiaDetailsRequest request) {
        Baia baia = find(id);
        String identificacao = normalizedIdentification(request.identificacao());
        repository.findByIdentificacaoIgnoreCase(identificacao)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResourceAlreadyExistsException("A bay with this identification already exists.");
                });
        String observacao = null;
        if (request.observacao() != null && !request.observacao().isBlank()) {
            observacao = request.observacao().trim();
        }
        baia.updateDetails(identificacao, observacao);
        try {
            return BaiaResponse.from(repository.saveAndFlush(baia));
        } catch (DataIntegrityViolationException exception) {
            throw new ResourceAlreadyExistsException("A bay with this identification already exists.");
        }
    }

    @Transactional
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN')")
    public BaiaResponse changeStatus(Long id, BaiaStatusRequest request) {
        Baia baia = findForUpdate(id);
        try {
            baia.changeStatusFromAdministration(request.status());
        } catch (IllegalStateException exception) {
            throw new OperationConflictException(exception.getMessage());
        }
        return BaiaResponse.from(baia);
    }

    @Transactional
    public void occupyAutomatically(Long id) {
        Baia baia = findForUpdate(id);
        try {
            baia.occupyAutomatically();
        } catch (IllegalStateException exception) {
            throw new OperationConflictException(exception.getMessage());
        }
    }

    @Transactional
    public void releaseAutomatically(Long id) {
        Baia baia = findForUpdate(id);
        try {
            baia.releaseAutomatically();
        } catch (IllegalStateException exception) {
            throw new OperationConflictException(exception.getMessage());
        }
    }

    private Baia find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bay not found."));
    }

    private Baia findForUpdate(Long id) {
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bay not found."));
    }

    private String normalizedIdentification(String value) {
        return value.trim();
    }

}
