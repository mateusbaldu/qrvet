package ipiranga.fatec.qrvet.repositories;

import ipiranga.fatec.qrvet.models.Baia;
import ipiranga.fatec.qrvet.models.enums.BaiaStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface BaiaRepository extends JpaRepository<Baia, Long> {
    boolean existsByIdentificacaoIgnoreCase(String identificacao);
    Optional<Baia> findByIdentificacaoIgnoreCase(String identificacao);
    Page<Baia> findAllByStatus(BaiaStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Baia b where b.id = :id")
    Optional<Baia> findByIdForUpdate(@Param("id") Long id);
}
