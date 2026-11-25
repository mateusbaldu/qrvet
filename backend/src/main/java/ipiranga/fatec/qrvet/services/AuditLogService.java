package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.filter.AuditLogFilter;
import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.dtos.response.AuditActionSummary;
import ipiranga.fatec.qrvet.dtos.response.AuditLogReportRow;
import ipiranga.fatec.qrvet.dtos.response.AuditLogResponse;
import ipiranga.fatec.qrvet.dtos.response.AuditResourceSummary;
import ipiranga.fatec.qrvet.dtos.response.PageResponse;
import ipiranga.fatec.qrvet.models.enums.AuditAction;
import ipiranga.fatec.qrvet.models.enums.AuditResource;
import ipiranga.fatec.qrvet.repositories.AuditLogRepository;
import ipiranga.fatec.qrvet.specifications.AuditLogSpecifications;
import ipiranga.fatec.qrvet.utils.PaginationUtils;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

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

    @Transactional(readOnly = true)
    public byte[] exportCsv(AuditLogFilter filter) {
        AuditLogFilter safeFilter = normalizeFilter(filter);
        String actionPattern = likePattern(safeFilter.action());
        String resourcePattern = likePattern(safeFilter.resourceName());

        List<AuditActionSummary> byAction = repository.countByAction(
                safeFilter.startDate(), safeFilter.endDate(), safeFilter.userId(), actionPattern, resourcePattern);
        List<AuditResourceSummary> byResource = repository.countByResource(
                safeFilter.startDate(), safeFilter.endDate(), safeFilter.userId(), actionPattern, resourcePattern);
        List<AuditLogReportRow> rows = repository.findReportRows(
                safeFilter.startDate(), safeFilter.endDate(), safeFilter.userId(), actionPattern, resourcePattern);

        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');

        csv.append("RESUMO POR AÇÃO\r\n");
        csv.append("Ação,Código,Total\r\n");
        for (AuditActionSummary item : byAction) {
            csv.append(csvCell(AuditAction.labelFor(item.getAction()))).append(',')
                    .append(csvCell(item.getAction())).append(',')
                    .append(item.getTotal()).append("\r\n");
        }
        csv.append("\r\n");

        csv.append("RESUMO POR RECURSO\r\n");
        csv.append("Recurso,Código,Total\r\n");
        for (AuditResourceSummary item : byResource) {
            csv.append(csvCell(AuditResource.labelFor(item.getResourceName()))).append(',')
                    .append(csvCell(item.getResourceName())).append(',')
                    .append(item.getTotal()).append("\r\n");
        }
        csv.append("\r\n");

        csv.append("REGISTROS DE AUDITORIA\r\n");
        csv.append("ID,Data/Hora (UTC),ID Usuário,E-mail Usuário,Endereço IP,Ação,Recurso,ID Recurso,Detalhes\r\n");
        for (AuditLogReportRow row : rows) {
            csv.append(numberOrEmpty(row.getId())).append(',')
                    .append(csvCell(instantOrEmpty(row.getTimestamp()))).append(',')
                    .append(numberOrEmpty(row.getUserId())).append(',')
                    .append(csvCell(row.getUserEmail())).append(',')
                    .append(csvCell(row.getIpAddress())).append(',')
                    .append(csvCell(AuditAction.labelFor(row.getAction()))).append(',')
                    .append(csvCell(AuditResource.labelFor(row.getResourceName()))).append(',')
                    .append(csvCell(row.getResourceId())).append(',')
                    .append(csvCell(row.getDetails())).append("\r\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private AuditLogFilter normalizeFilter(AuditLogFilter filter) {
        if (filter == null) {
            return new AuditLogFilter(null, null, null, null, null);
        }
        return filter;
    }

    private String numberOrEmpty(Long value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    private String instantOrEmpty(Instant value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    private String likePattern(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private String csvCell(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String sanitized = value;
        char first = sanitized.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@') {
            sanitized = "'" + sanitized;
        }
        return "\"" + sanitized.replace("\"", "\"\"") + "\"";
    }
}
