package ipiranga.fatec.qrvet.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ipiranga.fatec.qrvet.event.AuditEvent;
import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private final ApplicationEventPublisher events;
    private final UserRepository users;

    public AuditService(ApplicationEventPublisher events, UserRepository users) {
        this.events = events;
        this.users = users;
    }

    public void log(String action, String resourceName, String resourceId, Object details) {
        HttpServletRequest request = resolveRequest();
        log(request, action, resourceName, resourceId, details);
    }

    public void log(HttpServletRequest request, String action, String resourceName, String resourceId, Object details) {
        User currentUser = resolveCurrentUser();
        events.publishEvent(new AuditEvent(
                currentUser != null ? currentUser.getId() : null,
                currentUser != null ? currentUser.getEmail() : null,
                extractClientIpAddress(request),
                action,
                resourceName,
                resourceId,
                details));
    }

    private User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            return null;
        }

        String principalName = authentication.getName();
        try {
            Long userId = Long.parseLong(principalName);
            return users.findById(userId).orElse(null);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private HttpServletRequest resolveRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private String extractClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }

        return request.getRemoteAddr();
    }
}
