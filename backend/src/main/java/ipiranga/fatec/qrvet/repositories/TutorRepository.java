package ipiranga.fatec.qrvet.repositories;

import ipiranga.fatec.qrvet.models.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface TutorRepository extends JpaRepository<Tutor, Long>, JpaSpecificationExecutor<Tutor> {
    Optional<Tutor> findByCpf(String cpf);
    Optional<Tutor> findByEmailIgnoreCase(String email);
}
