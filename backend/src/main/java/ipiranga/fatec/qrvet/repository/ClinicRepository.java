package ipiranga.fatec.qrvet.repository;

import ipiranga.fatec.qrvet.model.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ClinicRepository extends JpaRepository<Clinic, UUID> {}
