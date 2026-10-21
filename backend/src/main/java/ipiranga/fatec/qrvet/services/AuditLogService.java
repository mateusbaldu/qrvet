package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.filter.AuditLogFilter;
import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.dtos.response.AuditLogResponse;
import ipiranga.fatec.qrvet.dtos.response.PageResponse;
import ipiranga.fatec.qrvet.repositories.AuditLogRepository;
import ipiranga.fatec.qrvet.specifications.AuditLogSpecifications;
import ipiranga.fatec.qrvet.utils.PaginationUtils;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogService {
    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> findLogs(
            AuditLogFilter filter,
            PaginationRequest pagination) {
        return PageResponse.from(repository.findAll(
                        AuditLogSpecifications.matches(filter),
                        PaginationUtils.byId(pagination, Sort.Direction.DESC))
                .map(AuditLogResponse::from));
    }
}
