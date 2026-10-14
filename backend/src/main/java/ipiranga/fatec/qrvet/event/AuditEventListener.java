package ipiranga.fatec.qrvet.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ipiranga.fatec.qrvet.models.AuditLog;
import ipiranga.fatec.qrvet.repositories.AuditLogRepository;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditEventListener {
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final AuditDetailsSanitizer sanitizer;

    public AuditEventListener(
            AuditLogRepository auditLogRepository,
            ObjectMapper objectMapper,
            AuditDetailsSanitizer sanitizer) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.sanitizer = sanitizer;
    }

    @Async
    @EventListener
    @Transactional
    public void handle(AuditEvent event) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(event.userId());
        auditLog.setUserEmail(event.userEmail());
        auditLog.setIpAddress(event.ipAddress());
        auditLog.setAction(event.action());
        auditLog.setResourceName(event.resourceName());
        auditLog.setResourceId(event.resourceId());

        if (event.details() != null) {
            try {
                auditLog.setDetails(objectMapper.writeValueAsString(sanitizer.sanitize(event.details())));
            } catch (JsonProcessingException e) {
                auditLog.setDetails("{\"error\":\"details_unavailable\"}");
            }
        }

        auditLogRepository.save(auditLog);
    }
}
