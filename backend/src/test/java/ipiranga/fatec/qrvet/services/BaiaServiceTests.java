package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.request.BaiaDetailsRequest;
import ipiranga.fatec.qrvet.dtos.request.BaiaRequest;
import ipiranga.fatec.qrvet.dtos.request.BaiaStatusRequest;
import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.exceptions.OperationConflictException;
import ipiranga.fatec.qrvet.exceptions.ResourceAlreadyExistsException;
import ipiranga.fatec.qrvet.exceptions.ResourceNotFoundException;
import ipiranga.fatec.qrvet.models.Baia;
import ipiranga.fatec.qrvet.models.enums.BaiaStatus;
import ipiranga.fatec.qrvet.repositories.BaiaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BaiaServiceTests {
    private final BaiaRepository repository = mock(BaiaRepository.class);
    private final BaiaService service = new BaiaService(repository);

    @Test
    void createsAvailableBayAndNormalizesFields() {
        Baia saved = Baia.builder().identificacao("A1").observacao("clean").build();
        when(repository.saveAndFlush(any(Baia.class))).thenReturn(saved);

        var result = service.create(new BaiaRequest(" A1 ", " clean ", null));

        assertEquals("A1", result.identificacao());
        assertEquals(BaiaStatus.DISPONIVEL, result.status());
        assertEquals("clean", result.observacao());
        verify(repository).saveAndFlush(any(Baia.class));
    }

    @Test
    void rejectsDuplicateAndOccupiedCreation() {
        when(repository.existsByIdentificacaoIgnoreCase("A1")).thenReturn(true);
        assertThrows(ResourceAlreadyExistsException.class,
                () -> service.create(new BaiaRequest(" A1 ", null, null)));

        when(repository.existsByIdentificacaoIgnoreCase("A2")).thenReturn(false);
        assertThrows(OperationConflictException.class,
                () -> service.create(new BaiaRequest("A2", null, BaiaStatus.OCUPADA)));
    }

    @Test
    void translatesPersistenceDuplicateToDomainException() {
        when(repository.saveAndFlush(any(Baia.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(ResourceAlreadyExistsException.class,
                () -> service.create(new BaiaRequest("A1", null, BaiaStatus.DISPONIVEL)));
    }

    @Test
    void listsAllOrFilteredBays() {
        Baia bay = Baia.builder().identificacao("A1").build();
        setId(bay, 1L);
        when(repository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(bay)));
        when(repository.findAllByStatus(eq(BaiaStatus.MANUTENCAO), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(bay)));

        assertEquals(1, service.list(new PaginationRequest(0, 20), null).items().size());
        assertEquals(1, service.list(new PaginationRequest(0, 20), BaiaStatus.MANUTENCAO).items().size());
    }

    @Test
    void updatesBayAndRejectsAnotherBayWithSameName() {
        Baia bay = Baia.builder().identificacao("A1").build();
        when(repository.findById(1L)).thenReturn(Optional.of(bay));
        when(repository.findByIdentificacaoIgnoreCase("A2")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(bay)).thenReturn(bay);

        var result = service.update(1L, new BaiaDetailsRequest(" A2 ", " "));

        assertEquals("A2", result.identificacao());
        assertNull(result.observacao());
        Baia other = Baia.builder().identificacao("A2").build();
        setId(other, 2L);
        when(repository.findByIdentificacaoIgnoreCase("A3")).thenReturn(Optional.of(other));
        assertThrows(ResourceAlreadyExistsException.class,
                () -> service.update(1L, new BaiaDetailsRequest("A3", null)));
    }

    @Test
    void changesStatusAndMapsInvalidTransitionsToConflict() {
        Baia available = Baia.builder().identificacao("A1").build();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(available));

        assertEquals(BaiaStatus.MANUTENCAO,
                service.changeStatus(1L, new BaiaStatusRequest(BaiaStatus.MANUTENCAO)).status());

        Baia occupied = Baia.builder().identificacao("A2").status(BaiaStatus.OCUPADA).build();
        when(repository.findByIdForUpdate(2L)).thenReturn(Optional.of(occupied));
        assertThrows(OperationConflictException.class,
                () -> service.changeStatus(2L, new BaiaStatusRequest(BaiaStatus.DISPONIVEL)));
    }

    @Test
    void occupiesAndReleasesAutomatically() {
        Baia bay = Baia.builder().identificacao("A1").build();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(bay));
        service.occupyAutomatically(1L);
        assertEquals(BaiaStatus.OCUPADA, bay.getStatus());

        service.releaseAutomatically(1L);
        assertEquals(BaiaStatus.DISPONIVEL, bay.getStatus());
    }

    @Test
    void reportsMissingBayAndInvalidAutomaticTransition() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(9L));
        when(repository.findByIdForUpdate(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.occupyAutomatically(9L));

        Baia maintenance = Baia.builder().identificacao("A1").status(BaiaStatus.MANUTENCAO).build();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(maintenance));
        assertThrows(OperationConflictException.class, () -> service.occupyAutomatically(1L));
    }

    private static void setId(Baia bay, Long id) {
        try {
            Field field = Baia.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(bay, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
