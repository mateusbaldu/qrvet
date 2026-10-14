package ipiranga.fatec.qrvet.event;

public record AuditEvent(
        Long userId,
        String userEmail,
        String ipAddress,
        String action,
        String resourceName,
        String resourceId,
        Object details) {
}
