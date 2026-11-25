package ipiranga.fatec.qrvet.repositories;

import ipiranga.fatec.qrvet.models.Evolucao;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EvolucaoRepository extends JpaRepository<Evolucao, Long> {
    Page<Evolucao> findAllByInternacaoId(Long internacaoId, Pageable pageable);

    @Query("""
            SELECT e FROM Evolucao e
            JOIN FETCH e.usuario
            JOIN FETCH e.internacao i
            WHERE (:startDate IS NULL OR e.dataHoraRegistro >= :startDate)
              AND (:endDate IS NULL OR e.dataHoraRegistro <= :endDate)
              AND (:veterinarioId IS NULL OR i.veterinario.id = :veterinarioId)
            ORDER BY e.dataHoraRegistro DESC, e.id DESC
            """)
    List<Evolucao> findForReport(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("veterinarioId") Long veterinarioId);
}
