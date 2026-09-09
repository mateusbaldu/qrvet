package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.dtos.request.ResetPasswordRequest;
import ipiranga.fatec.qrvet.dtos.request.UserRequest;
import ipiranga.fatec.qrvet.dtos.filter.UserSearchFilter;
import ipiranga.fatec.qrvet.dtos.response.*;
import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.event.InvitationEmailRequested;
import ipiranga.fatec.qrvet.exceptions.*;
import ipiranga.fatec.qrvet.repositories.UserRepository;
import ipiranga.fatec.qrvet.specifications.UserSpecifications;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class UserService {
    private static final int MAX_PAGE_SIZE = 100;
    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final SessionService sessions;
    private final ApplicationEventPublisher events;
    private final StringRedisTemplate redis;

    public UserService(UserRepository repository, PasswordEncoder encoder, SessionService sessions, ApplicationEventPublisher events, StringRedisTemplate redis) {
        this.repository = repository;
        this.encoder = encoder;
        this.sessions = sessions;
        this.events = events;
        this.redis = redis;
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

        return UserResponse.from(newUser);
    }


    public void sendInvitationToUserById(Long id) {
        User u = repository.findUserByIdWithLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if (u.isConfirmed()) {
            throw new OperationConflictException("Sign up already confirmed.");
        }

        String resendKey = "invitation:reenviado:" + id;
        if (!Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(resendKey, "1", Duration.ofMinutes(1)))) {
            throw new OperationConflictException("Wait one minute before resending the invitation.");
        }

        String token = UUID.randomUUID().toString();
        redis.opsForValue().set("invitation:" + token, id.toString(), Duration.ofHours(24));
        try {
            events.publishEvent(new InvitationEmailRequested(u.getEmail(), u.getName(), token));
        } catch (org.springframework.mail.MailException e) {
            throw new OperationConflictException(
                    "The invitation could not be sent. Check the email service and try again.");
        }
    }

    @Transactional
    public void confirmUserInvitation(ResetPasswordRequest request) {
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidRequestException("Password must have a max of 72 characters.");
        }
        String value = redis.opsForValue().getAndDelete("invitation:" + request.token());

        if (value == null) {
            throw new InvalidRequestException("Invite already used or expired.");
        }
        User u = repository.findUserByIdWithLock(Long.valueOf(value))
                .orElseThrow(() -> new InvalidRequestException("Invite already used or expired."));
        if (u.isConfirmed()) {
            throw new InvalidRequestException("Invite already used or expired.");
        }
        u.setPassword(request.newPassword(), encoder);
        u.setConfirmed(true);
        u.setActive(true);
        u.setAuthenticationVersion(u.getAuthenticationVersion() + 1);
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
        return PageRequest.of(page, size, Sort.by("id").descending());
    }

}
