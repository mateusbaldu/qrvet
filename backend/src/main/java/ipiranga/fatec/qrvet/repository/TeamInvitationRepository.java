package ipiranga.fatec.qrvet.repository;

import ipiranga.fatec.qrvet.model.InvitationStatus;
import ipiranga.fatec.qrvet.model.TeamInvitation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, UUID> {
    List<TeamInvitation> findAllByClinicIdAndStatusOrderByInvitedAtAsc(UUID clinicId, InvitationStatus status);
    Optional<TeamInvitation> findByIdAndClinicId(UUID id, UUID clinicId);
    Optional<TeamInvitation> findByTokenHash(String tokenHash);
    boolean existsByClinicIdAndEmailIgnoreCaseAndStatus(UUID clinicId, String email, InvitationStatus status);
}
