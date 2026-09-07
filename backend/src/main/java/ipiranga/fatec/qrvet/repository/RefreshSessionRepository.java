package ipiranga.fatec.qrvet.repository;

import ipiranga.fatec.qrvet.model.RefreshSession;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {
    @EntityGraph(attributePaths = {"user", "user.clinic"})
    Optional<RefreshSession> findByTokenHash(String tokenHash);
    List<RefreshSession> findAllByUserId(UUID userId);
}
