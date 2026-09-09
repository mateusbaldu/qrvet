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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Internacao i where i.id = :id")
    Optional<Internacao> findByIdForUpdate(@Param("id") Long id);
}
