package ipiranga.fatec.qrvet.repositories;

import ipiranga.fatec.qrvet.models.Jejum;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JejumRepository extends JpaRepository<Jejum, Long> {
    boolean existsByInternacaoIdAndAtivoTrue(Long internacaoId);
}
