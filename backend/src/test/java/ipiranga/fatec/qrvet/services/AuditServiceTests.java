package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.event.AuditEvent;
import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.repositories.UserRepository;
import ipiranga.fatec.qrvet.services.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditServiceTests {
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final UserRepository users = mock(UserRepository.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final AuditService service = new AuditService(events, users);

    @BeforeEach
    void setUp() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(42L);
        when(user.getEmail()).thenReturn("admin@qrvet.test");
        when(users.findById(42L)).thenReturn(Optional.of(user));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("42", null, "ROLE_ADMIN"));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    void capturesCurrentUserAndRemoteIp() {
        when(request.getRemoteAddr()).thenReturn("192.0.2.10");

        service.log("LOGIN_SUCCESS", "auth", "42", null);

        verify(events).publishEvent(new AuditEvent(
                42L, "admin@qrvet.test", "192.0.2.10",
                "LOGIN_SUCCESS", "auth", "42", null));
    }

    @Test
    void prefersFirstForwardedIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.4, 10.0.0.2");

        service.log("LOGIN_SUCCESS", "auth", null, null);

        verify(events).publishEvent(any(AuditEvent.class));
        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(events).publishEvent(event.capture());
        assertEquals("198.51.100.4", event.getValue().ipAddress());
    }

    @Test
    void usesRealIpWhenForwardedHeaderIsMissing() {
        when(request.getHeader("X-Real-IP")).thenReturn("198.51.100.8");

        service.log("LOGIN_SUCCESS", "auth", null, null);

        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(events).publishEvent(event.capture());
        assertEquals("198.51.100.8", event.getValue().ipAddress());
    }

    @Test
    void publishesNullUserAndIpWithoutRequest() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();

        service.log("LOGIN_FAILED", "auth", null, null);

        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(events).publishEvent(event.capture());
        assertNull(event.getValue().userId());
        assertNull(event.getValue().userEmail());
        assertNull(event.getValue().ipAddress());
    }
}
