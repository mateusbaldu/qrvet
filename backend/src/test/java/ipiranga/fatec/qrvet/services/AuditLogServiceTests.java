package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.filter.AuditLogFilter;
import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.dtos.response.AuditLogResponse;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private AuditLog log(String action, String resource) {
        AuditLog log = new AuditLog();
        log.setId(1L);
        log.setAction(action);
        log.setResourceName(resource);
        return log;
    }
}
