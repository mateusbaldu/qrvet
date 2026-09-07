package ipiranga.fatec.qrvet.repository;

import ipiranga.fatec.qrvet.model.Role;
import ipiranga.fatec.qrvet.model.UserAccount;
import ipiranga.fatec.qrvet.model.UserStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    @Override
    @EntityGraph(attributePaths = "clinic")
    Optional<UserAccount> findById(UUID id);

    @EntityGraph(attributePaths = "clinic")
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<UserAccount> findAllByClinicIdAndStatusOrderByCreatedAtAsc(UUID clinicId, UserStatus status);
    Optional<UserAccount> findByIdAndClinicId(UUID id, UUID clinicId);
    long countByClinicIdAndRoleAndStatus(UUID clinicId, Role role, UserStatus status);
}
