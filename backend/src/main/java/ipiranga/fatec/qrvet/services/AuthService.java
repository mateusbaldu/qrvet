package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.AuthSession;
import ipiranga.fatec.qrvet.dtos.RefreshToken;
import ipiranga.fatec.qrvet.dtos.request.*;
import ipiranga.fatec.qrvet.dtos.response.*;
import ipiranga.fatec.qrvet.dtos.RecoveryData;
import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.event.PasswordRecoveryEmailRequested;
import ipiranga.fatec.qrvet.exceptions.*;
import ipiranga.fatec.qrvet.repositories.UserRepository;
import ipiranga.fatec.qrvet.security.CurrentUser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final CurrentUser current;
    private final SessionService sessoes;
    private final PasswordRecoveryService passwordRecovery;
    private final ApplicationEventPublisher events;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokens, CurrentUser current, SessionService sessoes, PasswordRecoveryService passwordRecovery, ApplicationEventPublisher events) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.current = current;
        this.sessoes = sessoes;
        this.passwordRecovery = passwordRecovery;
        this.events = events;
    }


    @Transactional
    public AuthSession login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim())
                .filter(v -> v.isActive() && v.isConfirmed())
                .orElseThrow(() ->  new BadCredentialsException("Invalid credentials"));
        if (!user.passwordMatches(request.password(), passwordEncoder)) {
            throw new BadCredentialsException("Invalid credentials");
        }

        Instant now = Instant.now();
        user.setLastActivityAt(now);
        RefreshToken refresh = tokens.generateRefreshToken(user, now);
        sessoes.registerSession(user.getId(), refresh.jti(), now);

        AccessTokenResponse accessToken = new AccessTokenResponse(tokens.generateAccessToken(user, now));

        return new AuthSession((accessToken), refresh.value());
    }

    @Transactional(readOnly = true)
    public AccessTokenResponse refreshAccessToken(String refreshToken) {
        Jwt jwt = tokens.decodeRefreshToken(refreshToken);
        return new AccessTokenResponse(tokens.generateAccessToken(refreshUser(jwt), Instant.now()));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser() {
        return UserResponse.from(current.getCurrent());
    }

    @Transactional
    public void heartbeat() {
        User user = users.findUserByIdWithLock(
                current.getCurrent().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setLastActivityAt(Instant.now());
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;

        Jwt jwt = tokens.decodeRefreshToken(refreshToken);
        sessoes.revokeSession(Long.valueOf(jwt.getSubject()), jwt.getId());
    }

    @Transactional
    public void changePassword(PasswordRequest request) {
        User user = users.findUserByIdWithLock(current.getCurrent().getId()).orElseThrow();

        if (!user.passwordMatches(request.currentPassword(), passwordEncoder))
            throw new BadCredentialsException("Invalid current password");

        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new InvalidRequestException("Password surpasses 72 bytes.");

        user.setPassword(request.newPassword(), passwordEncoder);
        user.setAuthenticationVersion(user.getAuthenticationVersion() + 1);
    }

    @Transactional
    public void requestRecovery(ForgotPasswordRequest request) {
        User dbUser = users.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email"));

        User user = users.findUserByIdWithLock(dbUser.getId()).orElseThrow();
        if (!user.isActive() || !user.isConfirmed()) return;
        if (!passwordRecovery.canCreate(user.getId())) return;

        String token = UUID.randomUUID().toString();
        passwordRecovery.save(token, user.getId(), user.getAuthenticationVersion());

        try {
            events.publishEvent(new PasswordRecoveryEmailRequested(user.getEmail(), token));
        } catch (org.springframework.mail.MailException e) {
            passwordRecovery.delete(token);
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new InvalidRequestException("Password must have a max of 72 bytes.");

        RecoveryData recovery = passwordRecovery.consume(request.token())
                        .orElseThrow(() -> new InvalidRequestException("Password recovery link is invalid or expired. Request a new password recovery."));
        User dbUser = users.findUserByIdWithLock(recovery.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!dbUser.isActive() || recovery.authenticationVersion() != dbUser.getAuthenticationVersion())
            throw new InvalidRequestException("Invalid accout, check if the user is active");

        dbUser.setPassword(request.newPassword(), passwordEncoder);
        dbUser.setAuthenticationVersion(dbUser.getAuthenticationVersion() + 1);
    }


    private User refreshUser(Jwt jwt) {
        Long id = Long.valueOf(jwt.getSubject());
        User dbUser = users.findById(id).filter(v -> v.isActive() && v.isConfirmed())
                        .orElseThrow(() -> new BadCredentialsException("Invalid session"));
        if (!sessoes.isSessionValid(id, jwt.getId()))
            throw new BadCredentialsException("Invalid session");
        return dbUser;
    }

}
