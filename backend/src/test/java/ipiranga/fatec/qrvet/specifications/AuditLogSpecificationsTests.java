package ipiranga.fatec.qrvet.specifications;

import ipiranga.fatec.qrvet.dtos.filter.AuditLogFilter;
import ipiranga.fatec.qrvet.models.AuditLog;
import ipiranga.fatec.qrvet.repositories.AuditLogRepository;
import ipiranga.fatec.qrvet.specifications.AuditLogSpecifications;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditLogSpecificationsTests {
    @Autowired AuditLogRepository auditLogs;

    private final Instant start = Instant.parse("2026-01-01T00:00:00Z");
    private final Instant end = Instant.parse("2026-01-31T23:59:59Z");

    @BeforeEach
    void setUp() {
        auditLogs.deleteAll();
        save(1L, "LOGIN_SUCCESS", "auth", start);
        save(2L, "PATIENT_CREATED", "patient", start.plusSeconds(1));
        save(2L, "PATIENT_UPDATED", "patient", end);
        save(3L, "HOSPITALIZATION_UPDATED", "hospitalization", end.plusSeconds(1));
    }

    @Test
    void filtersByEachField() {
        assertEquals(4, auditLogs.findAll(AuditLogSpecifications.matches(
                new AuditLogFilter(start, null, null, null, null))).size());
        assertEquals(2, auditLogs.findAll(AuditLogSpecifications.matches(
                new AuditLogFilter(null, null, 2L, null, null))).size());
        assertEquals(1, auditLogs.findAll(AuditLogSpecifications.matches(
                new AuditLogFilter(null, null, null, "created", null))).size());
        assertEquals(2, auditLogs.findAll(AuditLogSpecifications.matches(
                new AuditLogFilter(null, null, null, null, "PATIENT"))).size());
    }

    @Test
    void combinesFiltersAndIncludesDateBounds() {
        AuditLogFilter filter = new AuditLogFilter(start, end, 2L, "patient", "patient");

        List<AuditLog> result = auditLogs.findAll(AuditLogSpecifications.matches(filter));

        assertEquals(2, result.size());
        assertEquals("PATIENT_UPDATED", result.get(1).getAction());
    }

    @Test
    void ignoresNullAndBlankFilters() {
        List<AuditLog> all = auditLogs.findAll(AuditLogSpecifications.matches(null));
        List<AuditLog> blank = auditLogs.findAll(AuditLogSpecifications.matches(
                new AuditLogFilter(null, null, null, "  ", null)));

        assertEquals(4, all.size());
        assertEquals(4, blank.size());
    }

    private void save(Long userId, String action, String resource, Instant timestamp) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setUserEmail("user" + userId + "@qrvet.test");
        log.setAction(action);
        log.setResourceName(resource);
        log.setTimestamp(timestamp);
        auditLogs.save(log);
    }
}
