package ipiranga.fatec.qrvet.repositories;

import ipiranga.fatec.qrvet.models.RegistroMedicacao;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistroMedicacaoRepository extends JpaRepository<RegistroMedicacao, Long> {
    List<RegistroMedicacao> findAllByInternacaoIdOrderByDataHoraRegistroDesc(Long internacaoId);

    @Query("""
            SELECT m FROM RegistroMedicacao m
            JOIN FETCH m.usuario
            JOIN FETCH m.internacao i
            WHERE (:startDate IS NULL OR m.dataHoraRegistro >= :startDate)
              AND (:endDate IS NULL OR m.dataHoraRegistro <= :endDate)
              AND (:veterinarioId IS NULL OR i.veterinario.id = :veterinarioId)
            ORDER BY m.dataHoraRegistro DESC, m.id DESC
            """)
    List<RegistroMedicacao> findForReport(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("veterinarioId") Long veterinarioId);
}
