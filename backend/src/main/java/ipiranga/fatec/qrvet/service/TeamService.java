package ipiranga.fatec.qrvet.service;

import ipiranga.fatec.qrvet.common.ApiException;
import ipiranga.fatec.qrvet.common.TokenTools;
import ipiranga.fatec.qrvet.model.InvitationStatus;
import ipiranga.fatec.qrvet.model.Role;
import ipiranga.fatec.qrvet.model.TeamInvitation;
import ipiranga.fatec.qrvet.model.UserAccount;
import ipiranga.fatec.qrvet.model.UserStatus;
import ipiranga.fatec.qrvet.repository.TeamInvitationRepository;
import ipiranga.fatec.qrvet.repository.UserAccountRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamService {
    private final UserAccountRepository users;
    private final TeamInvitationRepository invitations;
    private final MailService mailService;

    public TeamService(UserAccountRepository users, TeamInvitationRepository invitations, MailService mailService) {
        this.users = users;
        this.invitations = invitations;
        this.mailService = mailService;
    }

    @Transactional
    public TeamData list(UUID clinicId) {
        List<UserAccount> members = users.findAllByClinicIdAndStatusOrderByCreatedAtAsc(clinicId, UserStatus.ACTIVE);
        List<TeamInvitation> pending = invitations
            .findAllByClinicIdAndStatusOrderByInvitedAtAsc(clinicId, InvitationStatus.PENDING);
        pending.stream().filter(TeamInvitation::isExpired).forEach(TeamInvitation::markExpired);
        return new TeamData(members, pending);
    }

    @Transactional
    public TeamInvitation invite(UserAccount actor, String email, Role requestedRole) {
        Role role = requestedRole == null ? Role.VETERINARIAN : requestedRole;
        String normalized = UserAccount.normalizeEmail(email);
        if (users.existsByEmailIgnoreCase(normalized)
            || invitations.existsByClinicIdAndEmailIgnoreCaseAndStatus(actor.getClinic().getId(), normalized,
                InvitationStatus.PENDING)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_IN_TEAM",
                "Este e-mail já pertence à equipe ou possui um convite pendente.");
        }
        String rawToken = TokenTools.randomToken();
        TeamInvitation invitation = invitations.save(new TeamInvitation(actor.getClinic(), normalized, role,
            TokenTools.sha256(rawToken), Instant.now().plus(Duration.ofDays(7))));
        mailService.sendInvitation(normalized, rawToken, actor.getClinic().getName());
        return invitation;
    }

    @Transactional
    public void resend(UserAccount actor, UUID invitationId) {
        TeamInvitation invitation = invitation(actor.getClinic().getId(), invitationId);
        if (invitation.getStatus() != InvitationStatus.PENDING && invitation.getStatus() != InvitationStatus.EXPIRED) {
            throw new ApiException(HttpStatus.CONFLICT, "INVITATION_NOT_PENDING", "Este convite não está pendente.");
        }
        String rawToken = TokenTools.randomToken();
        invitation.renew(TokenTools.sha256(rawToken), Instant.now().plus(Duration.ofDays(7)));
        mailService.sendInvitation(invitation.getEmail(), rawToken, actor.getClinic().getName());
    }

    @Transactional
    public void cancel(UUID clinicId, UUID invitationId) {
        TeamInvitation invitation = invitation(clinicId, invitationId);
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "INVITATION_NOT_PENDING", "Este convite não está pendente.");
        }
        invitation.cancel();
    }

    @Transactional
    public UserAccount changeRole(UUID clinicId, UUID memberId, Role role) {
        UserAccount member = member(clinicId, memberId);
        if (member.getRole() == Role.ADMIN && role != Role.ADMIN && isLastAdmin(clinicId)) {
            throw new ApiException(HttpStatus.CONFLICT, "LAST_ADMIN_ROLE_CHANGE_NOT_ALLOWED",
                "A clínica precisa manter pelo menos um administrador ativo.");
        }
        member.changeRole(role);
        return member;
    }

    @Transactional
    public void remove(UUID clinicId, UUID memberId) {
        UserAccount member = member(clinicId, memberId);
        if (member.getRole() == Role.ADMIN && isLastAdmin(clinicId)) {
            throw new ApiException(HttpStatus.CONFLICT, "LAST_ADMIN_REMOVAL_NOT_ALLOWED",
                "O único administrador ativo não pode ser removido.");
        }
        member.deactivate();
    }

    private boolean isLastAdmin(UUID clinicId) {
        return users.countByClinicIdAndRoleAndStatus(clinicId, Role.ADMIN, UserStatus.ACTIVE) <= 1;
    }

    private UserAccount member(UUID clinicId, UUID id) {
        return users.findByIdAndClinicId(id, clinicId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Membro não encontrado."));
    }

    private TeamInvitation invitation(UUID clinicId, UUID id) {
        return invitations.findByIdAndClinicId(id, clinicId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVITATION_NOT_FOUND", "Convite não encontrado."));
    }

    public record TeamData(List<UserAccount> members, List<TeamInvitation> invitations) {}
}
