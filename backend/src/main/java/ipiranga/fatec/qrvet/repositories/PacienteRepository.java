package ipiranga.fatec.qrvet.repositories;

import ipiranga.fatec.qrvet.models.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    Page<Paciente> findAllByTutorId(Long tutorId, Pageable pageable);
}
