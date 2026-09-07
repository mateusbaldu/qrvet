package ipiranga.fatec.qrvet.api;

import ipiranga.fatec.qrvet.model.Role;
import ipiranga.fatec.qrvet.model.TeamInvitation;
import ipiranga.fatec.qrvet.model.UserAccount;
import ipiranga.fatec.qrvet.model.UserStatus;
import ipiranga.fatec.qrvet.repository.UserAccountRepository;
import ipiranga.fatec.qrvet.security.AuthenticatedUser;
import ipiranga.fatec.qrvet.service.TeamService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/team")
public class TeamController {
    private final TeamService teamService;
    private final UserAccountRepository users;
    private final Duration onlineWindow;

    public TeamController(TeamService teamService, UserAccountRepository users,
                          @Value("${app.presence.online-seconds}") long onlineSeconds) {
        this.teamService = teamService;
        this.users = users;
        this.onlineWindow = Duration.ofSeconds(onlineSeconds);
    }

    @GetMapping("/members")
    public TeamResponse list(@AuthenticationPrincipal AuthenticatedUser principal) {
        TeamService.TeamData data = teamService.list(principal.clinicId());
        List<MemberResponse> items = new ArrayList<>();
        data.members().forEach(user -> items.add(MemberResponse.from(user, onlineWindow)));
        data.invitations().forEach(invitation -> items.add(MemberResponse.from(invitation)));

        Map<Role, Long> roles = new EnumMap<>(Role.class);
        for (Role role : Role.values()) {
            roles.put(role, data.members().stream()
                .filter(member -> member.getStatus() == UserStatus.ACTIVE && member.getRole() == role).count());
        }
        long pending = data.invitations().stream()
            .filter(invitation -> invitation.getStatus() == ipiranga.fatec.qrvet.model.InvitationStatus.PENDING).count();
        return new TeamResponse(items, new TeamSummary(data.members().size() + pending, roles, pending));
    }

    @PostMapping("/invitations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MemberResponse> invite(@AuthenticationPrincipal AuthenticatedUser principal,
                                                  @Valid @RequestBody InviteRequest request) {
        UserAccount actor = users.findById(principal.id()).orElseThrow();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(MemberResponse.from(teamService.invite(actor, request.email(), request.role())));
    }

    @PostMapping("/invitations/{id}/resend")
    @PreAuthorize("hasRole('ADMIN')")
    public MessageResponse resend(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        teamService.resend(users.findById(principal.id()).orElseThrow(), id);
        return new MessageResponse("Convite reenviado.");
    }

    @DeleteMapping("/invitations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancel(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        teamService.cancel(principal.clinicId(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/members/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public MemberResponse changeRole(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id,
                                     @Valid @RequestBody RoleRequest request) {
        return MemberResponse.from(teamService.changeRole(principal.clinicId(), id, request.role()), onlineWindow);
    }

    @DeleteMapping("/members/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        teamService.remove(principal.clinicId(), id);
        return ResponseEntity.noContent().build();
    }

    public record InviteRequest(@Email @NotBlank String email, Role role) {}
    public record RoleRequest(@NotNull Role role) {}
    public record MessageResponse(String message) {}
    public record TeamResponse(List<MemberResponse> items, TeamSummary summary) {}
    public record TeamSummary(long total, Map<Role, Long> roles, long pending) {}
    public record MemberResponse(UUID id, String name, String email, String avatarUrl, Role role,
                                 String status, Instant lastActivityAt, Instant createdAt,
                                 Instant expiresAt, boolean invitation) {
        static MemberResponse from(UserAccount user, Duration onlineWindow) {
            String avatar = user.getAvatarData() == null ? null : "/api/users/" + user.getId() + "/avatar";
            boolean online = user.getLastActivityAt() != null
                && user.getLastActivityAt().isAfter(Instant.now().minus(onlineWindow));
            return new MemberResponse(user.getId(), user.getName(), user.getEmail(), avatar, user.getRole(),
                online ? "ONLINE" : "INACTIVE", user.getLastActivityAt(), user.getCreatedAt(), null, false);
        }
        static MemberResponse from(TeamInvitation invitation) {
            return new MemberResponse(invitation.getId(), "Aguardando aceite", invitation.getEmail(), null,
                invitation.getRole(), invitation.getStatus().name(), null, invitation.getInvitedAt(),
                invitation.getExpiresAt(), true);
        }
    }
}
