package ipiranga.fatec.qrvet.repository;

import ipiranga.fatec.qrvet.model.RefreshSession;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {
    Optional<RefreshSession> findByTokenHash(String tokenHash);
    List<RefreshSession> findAllByUserId(UUID userId);
}
