package ipiranga.fatec.qrvet.repositories;

import ipiranga.fatec.qrvet.models.AlimentacaoAgendada;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

public interface AlimentacaoAgendadaRepository extends JpaRepository<AlimentacaoAgendada, Long> {
    boolean existsByInternacaoIdAndHorarioAndStatusNot(Long internacaoId, Instant horario, AlimentacaoAgendada.Status status);
    boolean existsByInternacaoIdAndHorarioAndStatusNotAndIdNot(Long internacaoId, Instant horario, AlimentacaoAgendada.Status status, Long id);
    List<AlimentacaoAgendada> findAllByInternacaoIdAndStatus(Long internacaoId, AlimentacaoAgendada.Status status);
    @Query("select a from AlimentacaoAgendada a join fetch a.internacao i join fetch i.paciente join fetch i.baia "
            + "left join fetch a.registro r left join fetch r.usuario "
            + "where a.horario >= :inicio and a.horario < :fim and (:internacaoId is null or i.id = :internacaoId) order by a.horario, a.id")
    List<AlimentacaoAgendada> checklist(@Param("inicio") Instant inicio, @Param("fim") Instant fim, @Param("internacaoId") Long internacaoId);

    @Query("""
            SELECT a FROM AlimentacaoAgendada a
            JOIN FETCH a.internacao i
            WHERE (:startDate IS NULL OR a.horario >= :startDate)
              AND (:endDate IS NULL OR a.horario <= :endDate)
              AND (:veterinarioId IS NULL OR i.veterinario.id = :veterinarioId)
            ORDER BY a.horario DESC, a.id DESC
            """)
    List<AlimentacaoAgendada> findForReport(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("veterinarioId") Long veterinarioId);
}
