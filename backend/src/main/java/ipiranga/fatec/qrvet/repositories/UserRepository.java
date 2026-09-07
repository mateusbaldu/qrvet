package ipiranga.fatec.qrvet.repositories;

import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.models.enums.Role;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface UserRepository
        extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
}