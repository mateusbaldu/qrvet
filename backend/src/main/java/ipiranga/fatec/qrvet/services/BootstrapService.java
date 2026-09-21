package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.request.BootstrapAdminRequest;
import ipiranga.fatec.qrvet.exceptions.InvalidRequestException;
import ipiranga.fatec.qrvet.exceptions.OperationConflictException;
import ipiranga.fatec.qrvet.exceptions.ResourceAlreadyExistsException;
import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.models.enums.Role;
import ipiranga.fatec.qrvet.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

@Service
public class BootstrapService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String secret;

    public BootstrapService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${qrvet.bootstrap-secret}") String secret) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.secret = secret;
    }

    @Transactional
    public void createAdmin(String providedSecret, BootstrapAdminRequest request) {
        validateSecret(providedSecret);

        if (users.countByRole(Role.ADMIN) > 0)
            throw new OperationConflictException("The admin account already exists.");

        persistAdmin(request);
    }

    @Transactional
    public boolean createInitialAdmin(BootstrapAdminRequest request) {
        if (users.countByRole(Role.ADMIN) > 0) return false;

        validateInitialAdmin(request);
        persistAdmin(request);
        return true;
    }

    private void persistAdmin(BootstrapAdminRequest request) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.findByEmailIgnoreCase(email).isPresent())
            throw new ResourceAlreadyExistsException("User with e-mail already exists.");

        User newUser = User.builder()
                .name(request.name().trim())
                .email(email)
                .role(Role.ADMIN)
                .active(true)
                .confirmed(true)
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();
        users.save(newUser);
    }

    private void validateInitialAdmin(BootstrapAdminRequest request) {
        if (request.name() == null || request.name().isBlank() || request.name().length() > 120) {
            throw new InvalidRequestException("Initial administrator name is invalid.");
        }
        if (request.email() == null
                || request.email().isBlank()
                || request.email().length() > 254
                || !request.email().contains("@")) {
            throw new InvalidRequestException("Initial administrator e-mail is invalid.");
        }
        if (request.password() == null
                || request.password().length() < 8
                || request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidRequestException(
                    "Initial administrator password must have between 8 and 72 bytes.");
        }
    }

    private void validateSecret(String providedSecret) {
        if (secret.isBlank()
                || providedSecret == null
                || !MessageDigest.isEqual(
                        secret.getBytes(StandardCharsets.UTF_8),
                        providedSecret.getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidRequestException("Invalid bootstrap secret.");
        }
    }
}
