package ipiranga.fatec.qrvet.repositories;

import ipiranga.fatec.qrvet.models.Internacao;
import ipiranga.fatec.qrvet.models.enums.InternacaoStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface InternacaoRepository extends JpaRepository<Internacao, Long> {
    boolean existsByPacienteIdAndStatus(Long pacienteId, InternacaoStatus status);
    Page<Internacao> findAllByStatus(InternacaoStatus status, Pageable pageable);
    Optional<Internacao> findByUuidToken(java.util.UUID uuidToken);

        @Query("select i from Internacao i where (:pacienteId is null or i.paciente.id = :pacienteId) "
            + "and (:status is null or i.status = :status) "
            + "and (:q is null or lower(i.paciente.nome) like lower(concat('%', :q, '%')) "
            + "or lower(i.baia.identificacao) like lower(concat('%', :q, '%')) "
            + "or lower(i.veterinario.name) like lower(concat('%', :q, '%')) "
            + "or str(i.id) like concat('%', :q, '%'))")
    Page<Internacao> search(@Param("pacienteId") Long pacienteId,
                    @Param("status") InternacaoStatus status,
                    @Param("q") String q,
                    Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Internacao i where i.id = :id")
    Optional<Internacao> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT i FROM Internacao i
            JOIN FETCH i.veterinario
            JOIN FETCH i.baia
            WHERE (:startDate IS NULL OR i.entradaInternacao >= :startDate)
              AND (:endDate IS NULL OR i.entradaInternacao <= :endDate)
              AND (:veterinarioId IS NULL OR i.veterinario.id = :veterinarioId)
            ORDER BY i.entradaInternacao DESC, i.id DESC
            """)
    List<Internacao> findForReport(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("veterinarioId") Long veterinarioId);

    @Query("""
            SELECT COUNT(i) FROM Internacao i
            WHERE i.saidaInternacao IS NOT NULL
              AND (:startDate IS NULL OR i.saidaInternacao >= :startDate)
              AND (:endDate IS NULL OR i.saidaInternacao <= :endDate)
              AND (:veterinarioId IS NULL OR i.veterinario.id = :veterinarioId)
            """)
    long countClosedInPeriod(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("veterinarioId") Long veterinarioId);
}
