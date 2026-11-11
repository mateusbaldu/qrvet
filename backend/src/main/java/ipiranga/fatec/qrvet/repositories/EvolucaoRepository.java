package ipiranga.fatec.qrvet.repositories;

import ipiranga.fatec.qrvet.models.Evolucao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvolucaoRepository extends JpaRepository<Evolucao, Long> {
    Page<Evolucao> findAllByInternacaoId(Long internacaoId, Pageable pageable);
}
