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
            @Value("${backend.bootstrap-secret}") String secret) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.secret = secret;
    }

    @Transactional
    public void createAdmin(String providedSecret, BootstrapAdminRequest request) {
        validateSecret(providedSecret);

        if (users.countByRole(Role.ADMIN) > 0)
            throw new OperationConflictException("The admin account already exists.");

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.findByEmailIgnoreCase(email).isPresent())
            throw new ResourceAlreadyExistsException("User with e-mail already exists.");

        User newUser = new User();
        newUser.setName(request.name().trim());
        newUser.setEmail(email);
        newUser.setRole(Role.ADMIN);
        newUser.setActive(true);
        newUser.setConfirmed(true);
        newUser.setPassword(request.password(), passwordEncoder);
        users.save(newUser);
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
