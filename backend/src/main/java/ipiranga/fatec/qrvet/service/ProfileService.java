package ipiranga.fatec.qrvet.service;

import ipiranga.fatec.qrvet.common.ApiException;
import ipiranga.fatec.qrvet.common.PasswordPolicy;
import ipiranga.fatec.qrvet.model.UserAccount;
import ipiranga.fatec.qrvet.repository.RefreshSessionRepository;
import ipiranga.fatec.qrvet.repository.UserAccountRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
    private static final Set<String> IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/webp");
    private final UserAccountRepository users;
    private final RefreshSessionRepository sessions;
    private final PasswordEncoder encoder;

    public ProfileService(UserAccountRepository users, RefreshSessionRepository sessions, PasswordEncoder encoder) {
        this.users = users;
        this.sessions = sessions;
        this.encoder = encoder;
    }

    @Transactional
    public UserAccount update(UUID userId, String name, String email, String phone, String crmv) {
        UserAccount user = user(userId);
        users.findByEmailIgnoreCase(email)
            .filter(existing -> !existing.getId().equals(userId))
            .ifPresent(existing -> { throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED",
                "Este e-mail já está cadastrado."); });
        user.updateProfile(name, email, phone, crmv);
        return user;
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        UserAccount user = user(userId);
        if (!encoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_CURRENT_PASSWORD",
                "A senha atual está incorreta.");
        }
        PasswordPolicy.requireValid(newPassword);
        user.changePassword(encoder.encode(newPassword));
        sessions.findAllByUserId(userId).forEach(ipiranga.fatec.qrvet.model.RefreshSession::revoke);
    }

    @Transactional
    public UserAccount updateAvatar(UUID userId, byte[] bytes, String contentType) {
        if (bytes.length == 0 || bytes.length > 5 * 1024 * 1024) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "AVATAR_TOO_LARGE",
                "A imagem deve ter no máximo 5 MB.");
        }
        String detected = detectImageType(bytes);
        if (detected == null || !IMAGE_TYPES.contains(contentType) || !detected.equals(contentType)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_AVATAR",
                "Envie uma imagem PNG, JPG ou WebP válida.");
        }
        UserAccount user = user(userId);
        user.updateAvatar(bytes, detected);
        return user;
    }

    @Transactional
    public void removeAvatar(UUID userId) { user(userId).removeAvatar(); }

    @Transactional
    public void heartbeat(UUID userId) { user(userId).markActivity(); }

    public UserAccount user(UUID id) {
        return users.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Usuário não encontrado."));
    }

    private String detectImageType(byte[] data) {
        if (data.length >= 8 && (data[0] & 0xff) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G')
            return "image/png";
        if (data.length >= 3 && (data[0] & 0xff) == 0xff && (data[1] & 0xff) == 0xd8 && (data[2] & 0xff) == 0xff)
            return "image/jpeg";
        if (data.length >= 12 && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
            && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P') return "image/webp";
        return null;
    }
}
