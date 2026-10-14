package ipiranga.fatec.qrvet.controllers;

import ipiranga.fatec.qrvet.dtos.filter.AuditLogFilter;
import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.dtos.response.AuditLogResponse;
import ipiranga.fatec.qrvet.dtos.response.PageResponse;
import ipiranga.fatec.qrvet.services.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {
    private final AuditLogService service;

    public AuditLogController(AuditLogService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<AuditLogResponse> list(
            @Valid @ModelAttribute PaginationRequest pagination,
            @ModelAttribute AuditLogFilter filter) {
        return service.findLogs(filter, pagination);
    }
}
