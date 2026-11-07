package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.filter.AuditLogFilter;
import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.dtos.response.AuditActionSummary;
import ipiranga.fatec.qrvet.dtos.response.AuditLogReportRow;
import ipiranga.fatec.qrvet.dtos.response.AuditLogResponse;
import ipiranga.fatec.qrvet.dtos.response.AuditResourceSummary;
import ipiranga.fatec.qrvet.dtos.response.PageResponse;
import ipiranga.fatec.qrvet.models.AuditLog;
import ipiranga.fatec.qrvet.repositories.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditLogServiceTests {
    private final AuditLogRepository repository = mock(AuditLogRepository.class);
    private final AuditLogService service = new AuditLogService(repository);

    @Test
    void mapsPagedResultsAndRequestsDescendingIdOrder() {
        AuditLog log = log("LOGIN_SUCCESS", "auth");
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(
                new PageImpl<>(List.of(log), PageRequest.of(1, 1, Sort.by(Sort.Direction.DESC, "id")), 2));

        PageResponse<AuditLogResponse> page = service.findLogs(new AuditLogFilter(null, null, null, null, null),
                new PaginationRequest(1, 1));

        assertEquals(1, page.items().size());
        assertEquals("Login realizado", page.items().get(0).action());
        assertEquals("autenticação", page.items().get(0).resourceName());
        assertEquals(1, page.page());
        assertEquals(2, page.pages());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), pageable.capture());
        assertEquals(Sort.Direction.DESC, pageable.getValue().getSort().getOrderFor("id").getDirection());
    }

    @Test
    void returnsEmptyPage() {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(
                new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        PageResponse<AuditLogResponse> page = service.findLogs(null, new PaginationRequest(0, 20));

        assertEquals(0, page.items().size());
        assertEquals(0, page.total());
        assertEquals(0, page.pages());
    }

    @Test
    void exportsCsvWithSummaryProjectionsAndSanitizedRows() {
        AuditActionSummary actionSummary = mock(AuditActionSummary.class);
        when(actionSummary.getAction()).thenReturn("LOGIN_SUCCESS");
        when(actionSummary.getTotal()).thenReturn(2L);

        AuditResourceSummary resourceSummary = mock(AuditResourceSummary.class);
        when(resourceSummary.getResourceName()).thenReturn("auth");
        when(resourceSummary.getTotal()).thenReturn(2L);

        AuditLogReportRow reportRow = mock(AuditLogReportRow.class);
        when(reportRow.getId()).thenReturn(10L);
        when(reportRow.getTimestamp()).thenReturn(Instant.parse("2026-01-10T10:00:00Z"));
        when(reportRow.getUserId()).thenReturn(1L);
        when(reportRow.getUserEmail()).thenReturn("admin@qrvet.test");
        when(reportRow.getIpAddress()).thenReturn("127.0.0.1");
        when(reportRow.getAction()).thenReturn("LOGIN_SUCCESS");
        when(reportRow.getResourceName()).thenReturn("auth");
        when(reportRow.getResourceId()).thenReturn("1");
        when(reportRow.getDetails()).thenReturn("=CMD()");

        when(repository.countByAction(any(), any(), any(), any(), any()))
                .thenReturn(List.of(actionSummary));
        when(repository.countByResource(any(), any(), any(), any(), any()))
                .thenReturn(List.of(resourceSummary));
        when(repository.findReportRows(any(), any(), any(), any(), any()))
                .thenReturn(List.of(reportRow));

        String csv = new String(
                service.exportCsv(new AuditLogFilter(null, null, 1L, "login", "auth")),
                StandardCharsets.UTF_8);

        assertTrue(csv.contains("RESUMO POR AÇÃO"));
        assertTrue(csv.contains("\"Login realizado\",\"LOGIN_SUCCESS\",2"));
        assertTrue(csv.contains("RESUMO POR RECURSO"));
        assertTrue(csv.contains("\"autenticação\",\"auth\",2"));
        assertTrue(csv.contains("REGISTROS DE AUDITORIA"));
        assertTrue(csv.contains("\"'=CMD()\""));
    }

    private AuditLog log(String action, String resource) {
        AuditLog log = new AuditLog();
        log.setId(1L);
        log.setAction(action);
        log.setResourceName(resource);
        return log;
    }
}
