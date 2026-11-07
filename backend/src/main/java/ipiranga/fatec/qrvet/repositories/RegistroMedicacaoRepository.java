package ipiranga.fatec.qrvet.repositories;

import ipiranga.fatec.qrvet.models.RegistroMedicacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroMedicacaoRepository extends JpaRepository<RegistroMedicacao, Long> {
    List<RegistroMedicacao> findAllByInternacaoIdOrderByDataHoraRegistroDesc(Long internacaoId);
}
