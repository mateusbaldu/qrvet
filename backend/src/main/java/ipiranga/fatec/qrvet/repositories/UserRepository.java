package ipiranga.fatec.qrvet.repositories;

import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.models.enums.Role;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface UserRepository
        extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from User e where e.id = :id")
    Optional<User> findUserByIdWithLock(@Param("id") Long id);

    Optional<User> findByEmailIgnoreCase(String email);

    long countByRole(Role role);

    Optional<User> findByIdAndRoleAndActiveTrue(Long id, Role role);
}