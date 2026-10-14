package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.request.TutorRequest;
import ipiranga.fatec.qrvet.exceptions.InvalidRequestException;
import ipiranga.fatec.qrvet.exceptions.ResourceAlreadyExistsException;
import ipiranga.fatec.qrvet.exceptions.ResourceNotFoundException;
import ipiranga.fatec.qrvet.models.Tutor;
import ipiranga.fatec.qrvet.repositories.TutorRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TutorServiceTests {
    private final TutorRepository repository = mock(TutorRepository.class);
    private final TutorService service = new TutorService(repository);

    @Test
    void createsTutorWithNormalizedValues() {
        Tutor saved = tutor(1L, "Ana", "52998224725", "ana@test");
        when(repository.saveAndFlush(any(Tutor.class))).thenReturn(saved);

        var result = service.create(new TutorRequest(
                " Ana ", "529.982.247-25", "(11) 99999-9999", " ANA@TEST ", " Rua A "));

        assertEquals("ana@test", result.email());
        assertEquals("52998224725", result.cpf());
        verify(repository).saveAndFlush(any(Tutor.class));
    }

    @Test
    void rejectsInvalidAndDuplicateTutorData() {
        TutorRequest request = new TutorRequest("Ana", "111.111.111-11", "1", "ana@test", "Rua");
        assertThrows(InvalidRequestException.class, () -> service.create(request));

        when(repository.findByCpf("52998224725")).thenReturn(Optional.of(tutor(1L, "Ana", "52998224725", "ana@test")));
        assertThrows(ResourceAlreadyExistsException.class, () -> service.create(
                new TutorRequest("Ana", "529.982.247-25", "1", "new@test", "Rua")));
    }

    @Test
    void updatesTutorAndRejectsMissingOrConflictingRecords() {
        Tutor current = tutor(1L, "Ana", "52998224725", "ana@test");
        when(repository.findById(1L)).thenReturn(Optional.of(current));
        when(repository.findByCpf("52998224725")).thenReturn(Optional.of(current));
        when(repository.findByEmailIgnoreCase("new@test")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(current)).thenReturn(current);

        var result = service.update(1L, new TutorRequest(
                " Ana Nova ", "529.982.247-25", "2", " NEW@TEST ", " Nova Rua "));

        assertEquals("Ana Nova", result.nome());
        assertEquals("new@test", result.email());
        assertEquals("Nova Rua", current.getEndereco());

        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.update(9L,
                new TutorRequest("A", "529.982.247-25", "1", "a@test", "R")));
    }

    private static Tutor tutor(Long id, String name, String cpf, String email) {
        Tutor tutor = Tutor.builder().nome(name).cpf(cpf).telefone("1").email(email).endereco("Rua").build();
        tutor.setId(id);
        return tutor;
    }
}
