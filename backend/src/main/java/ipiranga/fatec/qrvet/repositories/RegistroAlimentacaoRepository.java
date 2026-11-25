package ipiranga.fatec.qrvet.repositories;

import ipiranga.fatec.qrvet.models.RegistroAlimentacao;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistroAlimentacaoRepository
        extends JpaRepository<RegistroAlimentacao, Long>, JpaSpecificationExecutor<RegistroAlimentacao> {

    @Query("""
            SELECT r FROM RegistroAlimentacao r
            JOIN FETCH r.usuario
            JOIN FETCH r.internacao i
            WHERE (:startDate IS NULL OR r.dataHoraRegistro >= :startDate)
              AND (:endDate IS NULL OR r.dataHoraRegistro <= :endDate)
              AND (:veterinarioId IS NULL OR i.veterinario.id = :veterinarioId)
            ORDER BY r.dataHoraRegistro DESC, r.id DESC
            """)
    List<RegistroAlimentacao> findForReport(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("veterinarioId") Long veterinarioId);
}
