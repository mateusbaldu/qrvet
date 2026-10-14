package ipiranga.fatec.qrvet.security;

import ipiranga.fatec.qrvet.dtos.AuthSession;
import ipiranga.fatec.qrvet.dtos.RefreshToken;
import ipiranga.fatec.qrvet.dtos.request.LoginRequest;
import ipiranga.fatec.qrvet.dtos.request.PasswordRequest;
import ipiranga.fatec.qrvet.dtos.response.AccessTokenResponse;
import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.models.enums.Role;
import ipiranga.fatec.qrvet.models.enums.AuditAction;
import ipiranga.fatec.qrvet.models.enums.AuditResource;
import ipiranga.fatec.qrvet.services.AuditService;
import ipiranga.fatec.qrvet.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTests {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final TokenService tokens = mock(TokenService.class);
    private final CurrentUser current = mock(CurrentUser.class);
    private final SessionService sessions = mock(SessionService.class);
    private final PasswordRecoveryService recovery = mock(PasswordRecoveryService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final AuditService audit = mock(AuditService.class);
    private final AuthService service =
            new AuthService(users, encoder, tokens, current, sessions, recovery, events, audit);

    @Test
    void rejectsUnknownInactiveOrUnconfirmedCredentials() {
        when(users.findByEmailIgnoreCase("user@test")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginRequest(" user@test ", "password")));
        verify(audit).log(any(AuditAction.class), any(AuditResource.class), isNull(), any());
    }

    @Test
    void logsInActiveConfirmedUserAndRegistersRefreshSession() {
        User user = new User(7L, "User", "user@test", "hash", Role.ADMIN, true, 0, true);
        when(users.findByEmailIgnoreCase("user@test")).thenReturn(Optional.of(user));
        when(encoder.matches("password", "hash")).thenReturn(true);
        Instant issuedAt = Instant.parse("2026-01-01T10:00:00Z");
        when(tokens.generateRefreshToken(eq(user), any())).thenReturn(new RefreshToken("refresh", "jti"));
        when(tokens.generateAccessToken(eq(user), any())).thenReturn("access");

        AuthSession result = service.login(new LoginRequest(" user@test ", "password"));

        assertEquals("refresh", result.refreshToken());
        assertEquals("access", result.tokens().accessToken());
        verify(sessions).registerSession(eq(7L), eq("jti"), any());
    }

    @Test
    void ignoresBlankLogoutToken() {
        service.logout(" ");

        verifyNoInteractions(tokens, sessions);
    }

    @Test
    void returnsCurrentUserFromCurrentUserService() {
        User user = new User(7L, "User", "user@test", "hash", Role.ADMIN, true, 0, true);
        when(current.getCurrent()).thenReturn(user);

        assertEquals(7L, service.currentUser().id());
    }

    @Test
    void refreshesOnlyValidatedSession() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("7");
        when(jwt.getId()).thenReturn("jti");
        when(users.findById(7L)).thenReturn(Optional.of(
                new User(7L, "User", "user@test", "hash", Role.ADMIN, true, 0, true)));
        when(sessions.isSessionValid(7L, "jti")).thenReturn(true);
        when(tokens.decodeRefreshToken("refresh")).thenReturn(jwt);
        when(tokens.generateAccessToken(any(), any())).thenReturn("access");

        AccessTokenResponse response = service.refreshAccessToken("refresh");

        assertEquals("access", response.accessToken());
    }

    @Test
    void rejectsWrongCurrentPassword() {
        User user = new User(7L, "User", "user@test", "hash", Role.ADMIN, true, 0, true);
        when(current.getCurrent()).thenReturn(user);
        when(users.findUserByIdWithLock(7L)).thenReturn(Optional.of(user));
        when(encoder.matches("old", "hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> service.changePassword(new PasswordRequest("old", "newPassword")));
    }
}
