package ipiranga.fatec.qrvet.repositories;

import ipiranga.fatec.qrvet.models.Internacao;
import ipiranga.fatec.qrvet.models.enums.InternacaoStatus;
import jakarta.persistence.LockModeType;
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
}
