package ipiranga.fatec.qrvet.dtos.response;

import ipiranga.fatec.qrvet.models.AuditLog;
import ipiranga.fatec.qrvet.models.enums.AuditAction;
import ipiranga.fatec.qrvet.models.enums.AuditResource;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Instant timestamp,
        Long userId,
        String userEmail,
        String ipAddress,
        String action,
        String resourceName,
        String resourceId,
        String details) {
    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getTimestamp(),
                auditLog.getUserId(),
                auditLog.getUserEmail(),
                auditLog.getIpAddress(),
                AuditAction.labelFor(auditLog.getAction()),
                AuditResource.labelFor(auditLog.getResourceName()),
                auditLog.getResourceId(),
                auditLog.getDetails());
    }
}
