package ipiranga.fatec.qrvet.service;

import ipiranga.fatec.qrvet.common.ApiException;
import ipiranga.fatec.qrvet.common.PasswordPolicy;
import ipiranga.fatec.qrvet.common.TokenTools;
import ipiranga.fatec.qrvet.model.PasswordResetToken;
import ipiranga.fatec.qrvet.model.RefreshSession;
import ipiranga.fatec.qrvet.model.TeamInvitation;
import ipiranga.fatec.qrvet.model.UserAccount;
import ipiranga.fatec.qrvet.model.UserStatus;
import ipiranga.fatec.qrvet.repository.PasswordResetTokenRepository;
import ipiranga.fatec.qrvet.repository.RefreshSessionRepository;
import ipiranga.fatec.qrvet.repository.TeamInvitationRepository;
import ipiranga.fatec.qrvet.repository.UserAccountRepository;
import ipiranga.fatec.qrvet.security.JwtService;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserAccountRepository users;
    private final RefreshSessionRepository refreshSessions;
    private final PasswordResetTokenRepository resetTokens;
    private final TeamInvitationRepository invitations;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;
    private final Duration refreshDuration;

    public AuthService(UserAccountRepository users, RefreshSessionRepository refreshSessions,
                       PasswordResetTokenRepository resetTokens, TeamInvitationRepository invitations,
                       PasswordEncoder passwordEncoder, JwtService jwtService, MailService mailService,
                       @Value("${app.jwt.refresh-days}") long refreshDays) {
        this.users = users;
        this.refreshSessions = refreshSessions;
        this.resetTokens = resetTokens;
        this.invitations = invitations;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailService = mailService;
        this.refreshDuration = Duration.ofDays(refreshDays);
    }

    @Transactional
    public AuthResult login(String email, String password) {
        UserAccount user = users.findByEmailIgnoreCase(email)
            .filter(candidate -> passwordEncoder.matches(password, candidate.getPasswordHash()))
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                "E-mail ou senha inválidos."));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "USER_INACTIVE", "Este usuário está inativo.");
        }
        user.markActivity();
        return issueTokens(user);
    }

    @Transactional
    public AuthResult refresh(String rawToken) {
        RefreshSession session = refreshSessions.findByTokenHash(TokenTools.sha256(rawToken))
            .orElseThrow(() -> invalidRefresh());
        if (!session.isUsable() || session.getUser().getStatus() != UserStatus.ACTIVE) {
            session.revoke();
            throw invalidRefresh();
        }
        session.revoke();
        return issueTokens(session.getUser());
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        refreshSessions.findByTokenHash(TokenTools.sha256(rawToken)).ifPresent(RefreshSession::revoke);
    }

    @Transactional
    public void forgotPassword(String email) {
        users.findByEmailIgnoreCase(email)
            .filter(user -> user.getStatus() == UserStatus.ACTIVE)
            .ifPresent(user -> {
                String rawToken = TokenTools.randomToken();
                resetTokens.save(new PasswordResetToken(user, TokenTools.sha256(rawToken),
                    Instant.now().plus(Duration.ofMinutes(30))));
                mailService.sendPasswordReset(user.getEmail(), rawToken);
            });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordPolicy.requireValid(newPassword);
        PasswordResetToken token = resetTokens.findByTokenHash(TokenTools.sha256(rawToken))
            .orElseThrow(() -> invalidReset());
        if (!token.isUsable()) throw invalidReset();
        token.getUser().changePassword(passwordEncoder.encode(newPassword));
        token.use();
        refreshSessions.findAllByUserId(token.getUser().getId()).forEach(RefreshSession::revoke);
    }

    @Transactional
    public void acceptInvitation(String rawToken, String name, String password, String phone, String crmv) {
        PasswordPolicy.requireValid(password);
        TeamInvitation invitation = invitations.findByTokenHash(TokenTools.sha256(rawToken))
            .orElseThrow(() -> invalidInvitation());
        if (invitation.getStatus() != ipiranga.fatec.qrvet.model.InvitationStatus.PENDING || invitation.isExpired()) {
            if (invitation.isExpired()) invitation.markExpired();
            throw invalidInvitation();
        }
        if (users.existsByEmailIgnoreCase(invitation.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "Este e-mail já está cadastrado.");
        }
        UserAccount user = new UserAccount(invitation.getClinic(), name, invitation.getEmail(),
            passwordEncoder.encode(password), invitation.getRole());
        user.updateProfile(name, invitation.getEmail(), phone, crmv);
        users.save(user);
        invitation.accept();
    }

    private AuthResult issueTokens(UserAccount user) {
        String refresh = TokenTools.randomToken();
        refreshSessions.save(new RefreshSession(user, TokenTools.sha256(refresh),
            Instant.now().plus(refreshDuration)));
        return new AuthResult(jwtService.createAccessToken(user), jwtService.accessExpiresInSeconds(), refresh, user);
    }

    private ApiException invalidRefresh() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "A sessão expirou. Faça login novamente.");
    }
    private ApiException invalidReset() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_OR_EXPIRED_RESET_TOKEN",
            "O link de recuperação é inválido ou expirou.");
    }
    private ApiException invalidInvitation() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_OR_EXPIRED_INVITATION",
            "O convite é inválido ou expirou.");
    }

    public record AuthResult(String accessToken, long expiresIn, String refreshToken, UserAccount user) {}
}
