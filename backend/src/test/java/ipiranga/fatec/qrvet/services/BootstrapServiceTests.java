package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.request.BootstrapAdminRequest;
import ipiranga.fatec.qrvet.exceptions.InvalidRequestException;
import ipiranga.fatec.qrvet.exceptions.OperationConflictException;
import ipiranga.fatec.qrvet.models.enums.Role;
import ipiranga.fatec.qrvet.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BootstrapServiceTests {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final BootstrapService service = new BootstrapService(users, encoder, "secret");
    private final BootstrapAdminRequest request =
            new BootstrapAdminRequest(" Admin ", " ADMIN@TEST ", "password");

    @Test
    void createsInitialAdminWhenNoneExists() {
        when(users.countByRole(Role.ADMIN)).thenReturn(0L);
        when(users.findByEmailIgnoreCase("admin@test")).thenReturn(Optional.empty());
        when(encoder.encode("password")).thenReturn("hash");

        assertTrue(service.createInitialAdmin(request));

        verify(users).save(any());
        verify(encoder).encode("password");
    }

    @Test
    void doesNotCreateSecondInitialAdmin() {
        when(users.countByRole(Role.ADMIN)).thenReturn(1L);

        assertFalse(service.createInitialAdmin(request));
        verify(users, never()).save(any());
    }

    @Test
    void validatesSecretAndExistingAdminForExplicitCreation() {
        assertThrows(InvalidRequestException.class, () -> service.createAdmin("wrong", request));
        when(users.countByRole(Role.ADMIN)).thenReturn(1L);
        assertThrows(OperationConflictException.class, () -> service.createAdmin("secret", request));
    }

    @Test
    void validatesInitialAdminFields() {
        when(users.countByRole(Role.ADMIN)).thenReturn(0L);
        assertThrows(InvalidRequestException.class, () -> service.createInitialAdmin(
                new BootstrapAdminRequest(" ", "bad-email", "short")));
        assertThrows(InvalidRequestException.class, () -> service.createInitialAdmin(
                new BootstrapAdminRequest("Admin", "admin@test", "a".repeat(73))));
    }
}
