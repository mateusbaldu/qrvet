package ipiranga.fatec.qrvet.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.dtos.request.ResetPasswordRequest;
import ipiranga.fatec.qrvet.dtos.request.UserRequest;
import ipiranga.fatec.qrvet.dtos.request.UserRoleRequest;
import ipiranga.fatec.qrvet.dtos.request.UserStatusRequest;
import ipiranga.fatec.qrvet.models.enums.Role;
import org.springframework.security.core.context.SecurityContextHolder;
import ipiranga.fatec.qrvet.dtos.filter.UserSearchFilter;
import ipiranga.fatec.qrvet.dtos.response.*;
import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.event.InvitationEmailRequested;
import ipiranga.fatec.qrvet.exceptions.*;
import ipiranga.fatec.qrvet.repositories.UserRepository;
import ipiranga.fatec.qrvet.security.SessionService;
import ipiranga.fatec.qrvet.security.UserInvitationService;
import ipiranga.fatec.qrvet.specifications.UserSpecifications;
import ipiranga.fatec.qrvet.utils.PaginationUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN')")
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final int MAX_PAGE_SIZE = 100;
    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final SessionService sessions;
    private final ApplicationEventPublisher events;
    private final UserInvitationService invitations;

    public UserService(UserRepository repository, PasswordEncoder encoder, SessionService sessions, ApplicationEventPublisher events, UserInvitationService invitations) {
        this.repository = repository;
        this.encoder = encoder;
        this.sessions = sessions;
        this.events = events;
        this.invitations = invitations;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listAllUsers(
            PaginationRequest pagination,
            UserSearchFilter filter) {
        return PageResponse.from(
                repository.findAll(
                                UserSpecifications.matches(filter),
                                pageable(pagination.page(), pagination.size()))
                        .map(UserResponse::from));
    }

    @Transactional
    public UserResponse createUser(UserRequest userRequest) {
        String email = userRequest.email().trim().toLowerCase(Locale.ROOT);
        logger.info("Creating user account for email {}", email);
        if (repository.findByEmailIgnoreCase(email).isPresent())
            throw new ResourceAlreadyExistsException("User with this email already exists.");

        User newUser = User.builder()
                .name(userRequest.name().trim())
                .email(email)
                .role(userRequest.role())
                .active(false)
                .confirmed(false)
                .passwordHash(encoder.encode(UUID.randomUUID().toString()))
                .build();

        repository.saveAndFlush(newUser);
        sendInvitationToUserById(newUser.getId());

        logger.info("User {} created", newUser.getId());
        return UserResponse.from(newUser);
    }


    @Transactional
    public void sendInvitationToUserById(Long id) {
        logger.info("Sending invitation to user {}", id);
        User dbUser = repository.findUserByIdWithLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        dbUser.validateConfirmed();

        if (!invitations.canSend(id)) {
            throw new OperationConflictException("Wait one minute before resending the invitation.");
        }

        String token = UUID.randomUUID().toString();
        invitations.save(token, id);
        events.publishEvent(new InvitationEmailRequested(dbUser.getEmail(), dbUser.getName(), token));
    }

    @Transactional
    public UserResponse updateRole(Long id, UserRoleRequest request) {
        Long actorId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        if (actorId.equals(id)) {
            throw new OperationConflictException("Peça a outro administrador para alterar sua permissão.");
        }
        // Serialize administrative changes and recheck the actor after obtaining the lock.
        var admins = repository.findAdministratorsForUpdate();
        if (admins.stream().noneMatch(u -> u.getId().equals(actorId) && u.isActive() && u.isConfirmed())) {
            throw new org.springframework.security.access.AccessDeniedException("Apenas administradores podem alterar permissões.");
        }
        User member = repository.findUserByIdWithLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if (member.getRole() == request.role()) return UserResponse.from(member);
        Role previous = member.getRole();
        member.setRole(request.role());
        repository.saveAndFlush(member);
        events.publishEvent(new ipiranga.fatec.qrvet.event.AuditEvent(actorId,
                admins.stream().filter(u -> u.getId().equals(actorId)).findFirst().orElseThrow().getEmail(), null,
                "USER_ROLE_UPDATED", "auth", id.toString(),
                java.util.Map.of("previousRole", previous.name(), "role", request.role().name())));
        return UserResponse.from(member);
    }

    @Transactional
    public UserResponse updateStatus(Long id, UserStatusRequest request) {
        Long actorId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        if (actorId.equals(id)) {
            throw new OperationConflictException("Peça a outro administrador para alterar o status da sua conta.");
        }
        var admins = repository.findAdministratorsForUpdate();
        if (admins.stream().noneMatch(u -> u.getId().equals(actorId) && u.isActive() && u.isConfirmed())) {
            throw new org.springframework.security.access.AccessDeniedException("Apenas administradores podem alterar o status de contas.");
        }
        User member = repository.findUserByIdWithLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if (!member.isConfirmed()) {
            throw new OperationConflictException("Aguarde a confirmação do convite antes de alterar o status da conta.");
        }
        boolean targetActive = Boolean.TRUE.equals(request.active());
        if (member.isActive() == targetActive) {
            return UserResponse.from(member);
        }
        boolean previous = member.isActive();
        member.setActive(targetActive);
        if (!targetActive) {
            member.setAuthenticationVersion(member.getAuthenticationVersion() + 1);
            sessions.revokeAllSessions(id);
        }
        repository.saveAndFlush(member);
        events.publishEvent(new ipiranga.fatec.qrvet.event.AuditEvent(actorId,
                admins.stream().filter(u -> u.getId().equals(actorId)).findFirst().orElseThrow().getEmail(), null,
                "USER_STATUS_UPDATED", "auth", id.toString(),
                java.util.Map.of("previousActive", previous, "active", targetActive)));
        return UserResponse.from(member);
    }

    @Transactional
    @PreAuthorize("permitAll()")
    public void confirmUserInvitation(ResetPasswordRequest request) {
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidRequestException("Password must have a max of 72 characters.");
        }
        Long userId = invitations.consume(request.token())
                .orElseThrow(() -> new InvalidRequestException("Invite already used or expired."));

        User dbUser = repository.findUserByIdWithLock(userId)
                .orElseThrow(() -> new InvalidRequestException("Invite already used or expired."));
        if (dbUser.isConfirmed()) {
            throw new InvalidRequestException("Invite already used or expired.");
        }
        dbUser.setPassword(request.newPassword(), encoder);
        dbUser.setConfirmed(true);
        dbUser.setActive(true);
        dbUser.setAuthenticationVersion(dbUser.getAuthenticationVersion() + 1);
        logger.info("User {} confirmed invitation", dbUser.getId());
    }


    @Transactional(readOnly = true)
    public PageResponse<ActiveSessionResponse> listSessions(Long userId, PaginationRequest pagination) {
        repository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        Pageable pageable = pageable(pagination.page(), pagination.size());
        List<ActiveSessionResponse> sessionItems = sessions.listSessions(userId).stream()
                .map(s -> new ActiveSessionResponse(s.jti(), s.createdAt(), s.expiresAt()))
                .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                .toList();

        return PageResponse.from(new PageImpl<>(
                sessionItems.stream()
                        .skip(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .toList(),
                pageable,
                sessionItems.size()));
    }

    public void revokeSession(Long userId, String jti) {
        sessions.revokeSession(userId, jti);
    }

    public void revokeAllSessions(Long userId) {
        sessions.revokeAllSessions(userId);
    }

    private Pageable pageable(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidRequestException("Invalid page size.");
        }
        return PaginationUtils.byId(page, size);
    }

}
