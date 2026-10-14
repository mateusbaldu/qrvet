package ipiranga.fatec.qrvet.dtos.filter;

import java.time.Instant;

public record AuditLogFilter(
        Instant startDate,
        Instant endDate,
        Long userId,
        String action,
        String resourceName) {
}
