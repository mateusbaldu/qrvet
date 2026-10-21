package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.request.PacienteRequest;
import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.exceptions.ResourceNotFoundException;
import ipiranga.fatec.qrvet.models.Paciente;
import ipiranga.fatec.qrvet.models.Tutor;
import ipiranga.fatec.qrvet.models.enums.AuditAction;
import ipiranga.fatec.qrvet.models.enums.AuditResource;
import ipiranga.fatec.qrvet.repositories.PacienteRepository;
import ipiranga.fatec.qrvet.repositories.TutorRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PacienteServiceTests {
    private final PacienteRepository patients = mock(PacienteRepository.class);
    private final TutorRepository tutors = mock(TutorRepository.class);
    private final TutorService tutorService = mock(TutorService.class);
    private final AuditService audit = mock(AuditService.class);
    private final PacienteService service = new PacienteService(patients, tutors, tutorService, audit);

    @Test
    void createsPatientForExistingTutorAndAuditsIt() {
        Tutor tutor = Tutor.builder().nome("Tutor").cpf("52998224725").telefone("1")
                .email("tutor@test").endereco("Rua").build();
        Paciente saved = patient(1L, tutor, "Rex");
        when(tutors.findById(7L)).thenReturn(Optional.of(tutor));
        when(patients.saveAndFlush(any(Paciente.class))).thenReturn(saved);

        var result = service.create(request(7L, null, " Rex "));

        assertEquals(1L, result.id());
        assertEquals("Rex", result.nome());
        verify(audit).log(any(AuditAction.class), any(AuditResource.class), eq("1"), any());
    }

    @Test
    void createsPatientWithNewTutor() {
        Tutor tutor = Tutor.builder().nome("Tutor").cpf("52998224725").telefone("1")
                .email("tutor@test").endereco("Rua").build();
        when(tutorService.create(any())).thenReturn(
                new ipiranga.fatec.qrvet.dtos.response.TutorResponse(7L, "Tutor", "52998224725", "1", "tutor@test", "Rua", null));
        when(tutors.findById(7L)).thenReturn(Optional.of(tutor));
        when(patients.saveAndFlush(any(Paciente.class))).thenReturn(patient(1L, tutor, "Rex"));

        assertEquals(1L, service.create(request(null, new ipiranga.fatec.qrvet.dtos.request.TutorRequest(
                "Tutor", "529.982.247-25", "1", "tutor@test", "Rua"), "Rex")).id());
    }

    @Test
    void reportsMissingPatientAndTutor() {
        when(patients.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(1L));

        when(tutors.existsById(7L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class,
                () -> service.listByTutor(7L, new PaginationRequest(0, 20)));
    }

    @Test
    void listsPatientsByTutor() {
        Tutor tutor = Tutor.builder().nome("Tutor").cpf("52998224725").telefone("1")
                .email("tutor@test").endereco("Rua").build();
        when(tutors.existsById(7L)).thenReturn(true);
        when(patients.findAllByTutorId(eq(7L), any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of(patient(1L, tutor, "Rex"))));

        assertEquals(1, service.listByTutor(7L, new PaginationRequest(0, 20)).items().size());
    }

    private static PacienteRequest request(Long tutorId, ipiranga.fatec.qrvet.dtos.request.TutorRequest newTutor,
                                           String name) {
        return new PacienteRequest(tutorId, name, "Canino", "SRD", "Macho",
                LocalDate.of(2020, 1, 1), BigDecimal.valueOf(10), null, newTutor);
    }

    private static Paciente patient(Long id, Tutor tutor, String name) {
        Paciente patient = Paciente.builder().tutor(tutor).nome(name).especie("Canino")
                .raca("SRD").sexo("Macho").dataNascimento(LocalDate.of(2020, 1, 1))
                .peso(BigDecimal.TEN).build();
        patient.setId(id);
        return patient;
    }
}
