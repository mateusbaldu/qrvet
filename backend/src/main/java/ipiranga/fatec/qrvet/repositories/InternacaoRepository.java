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
            + "and (:status is null or i.status = :status)")
    Page<Internacao> search(@Param("pacienteId") Long pacienteId,
                            @Param("status") InternacaoStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Internacao i where i.id = :id")
    Optional<Internacao> findByIdForUpdate(@Param("id") Long id);
}
