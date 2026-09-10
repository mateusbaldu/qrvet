package ipiranga.fatec.qrvet.repositories;

import ipiranga.fatec.qrvet.models.Jejum;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JejumRepository extends JpaRepository<Jejum, Long> {
    boolean existsByInternacaoIdAndAtivoTrue(Long internacaoId);

    List<Jejum> findAllByInternacaoIdOrderByDataHoraInicioDesc(Long internacaoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from Jejum j where j.internacao.id = :internacaoId and j.ativo = true")
    Optional<Jejum> findActiveForUpdate(@Param("internacaoId") Long internacaoId);
}
