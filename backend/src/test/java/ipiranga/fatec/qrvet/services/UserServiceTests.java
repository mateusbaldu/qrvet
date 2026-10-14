package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.dtos.request.ResetPasswordRequest;
import ipiranga.fatec.qrvet.dtos.request.UserRequest;
import ipiranga.fatec.qrvet.dtos.response.PageResponse;
import ipiranga.fatec.qrvet.exceptions.InvalidRequestException;
import ipiranga.fatec.qrvet.exceptions.ResourceAlreadyExistsException;
import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.models.enums.Role;
import ipiranga.fatec.qrvet.repositories.UserRepository;
import ipiranga.fatec.qrvet.security.SessionService;
import ipiranga.fatec.qrvet.security.UserInvitationService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceTests {
    private final UserRepository repository = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final SessionService sessions = mock(SessionService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final UserInvitationService invitations = mock(UserInvitationService.class);
    private final UserService service = new UserService(repository, encoder, sessions, events, invitations);

    @Test
    void listsUsersWithPagination() {
        User user = user(1L, "Ana", "ana@test", true, true);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        PageResponse<?> result = service.listAllUsers(new PaginationRequest(0, 20), null);

        assertEquals(1, result.items().size());
        assertEquals("ana@test", ((ipiranga.fatec.qrvet.dtos.response.UserResponse) result.items().get(0)).email());
    }

    @Test
    void rejectsDuplicateEmailBeforePersisting() {
        when(repository.findByEmailIgnoreCase("ana@test")).thenReturn(Optional.of(user(1L, "Ana", "ana@test", true, true)));

        assertThrows(ResourceAlreadyExistsException.class,
                () -> service.createUser(new UserRequest(" Ana ", " ANA@TEST ", Role.ADMIN)));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void listsSessionsSortedNewestFirstAndPaginates() {
        User user = user(1L, "Ana", "ana@test", true, true);
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        Instant old = Instant.parse("2026-01-01T10:00:00Z");
        Instant recent = old.plusSeconds(60);
        when(sessions.listSessions(1L)).thenReturn(List.of(
                new SessionService.SessionData("old", old, old.plusSeconds(100)),
                new SessionService.SessionData("recent", recent, recent.plusSeconds(100))));

        var result = service.listSessions(1L, new PaginationRequest(0, 1));

        assertEquals(1, result.items().size());
        assertEquals("recent", result.items().get(0).jti());
        assertEquals(2, result.total());
    }

    @Test
    void rejectsInvalidPaginationAndMissingUser() {
        assertThrows(InvalidRequestException.class,
                () -> service.listAllUsers(new PaginationRequest(-1, 20), null));
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ipiranga.fatec.qrvet.exceptions.ResourceNotFoundException.class,
                () -> service.listSessions(1L, new PaginationRequest(0, 20)));
    }

    @Test
    void delegatesSessionRevocation() {
        service.revokeSession(1L, "jti");
        service.revokeAllSessions(1L);

        verify(sessions).revokeSession(1L, "jti");
        verify(sessions).revokeAllSessions(1L);
    }

    @Test
    void confirmsInvitationAndUpdatesAccountState() {
        User user = user(1L, "Ana", "ana@test", false, false);
        when(invitations.consume("token")).thenReturn(Optional.of(1L));
        when(repository.findUserByIdWithLock(1L)).thenReturn(Optional.of(user));
        when(encoder.encode("password")).thenReturn("encoded");

        service.confirmUserInvitation(new ResetPasswordRequest("token", "password"));

        assertTrue(user.isActive());
        assertTrue(user.isConfirmed());
        assertEquals(1, user.getAuthenticationVersion());
        verify(encoder).encode("password");
    }

    @Test
    void rejectsOverlongInvitationPassword() {
        String password = "a".repeat(73);

        assertThrows(InvalidRequestException.class,
                () -> service.confirmUserInvitation(new ResetPasswordRequest("token", password)));
        verifyNoInteractions(invitations);
    }

    private static User user(Long id, String name, String email, boolean active, boolean confirmed) {
        return new User(id, name, email, "hash", Role.VETERINARIO, active, 0, confirmed);
    }
}
