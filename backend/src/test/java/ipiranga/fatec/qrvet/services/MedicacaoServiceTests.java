package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.request.RegistroMedicacaoRequest;
import ipiranga.fatec.qrvet.exceptions.InvalidRequestException;
import ipiranga.fatec.qrvet.exceptions.ResourceNotFoundException;
import ipiranga.fatec.qrvet.models.Internacao;
import ipiranga.fatec.qrvet.models.RegistroMedicacao;
import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.models.enums.InternacaoStatus;
import ipiranga.fatec.qrvet.models.enums.Role;
import ipiranga.fatec.qrvet.repositories.InternacaoRepository;
import ipiranga.fatec.qrvet.repositories.RegistroMedicacaoRepository;
import ipiranga.fatec.qrvet.security.CurrentUser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MedicacaoServiceTests {
    private final RegistroMedicacaoRepository medicacaoRepository = mock(RegistroMedicacaoRepository.class);
    private final InternacaoRepository internacaoRepository = mock(InternacaoRepository.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final MedicacaoService service =
            new MedicacaoService(medicacaoRepository, internacaoRepository, currentUser);

    @Test
    void registersMedicationForActiveHospitalization() {
        Internacao internacao = Internacao.builder()
                .motivo("Observação")
                .diagnosticoInicial("Avaliação")
                .build();
        internacao.setId(1L);
        User user = new User(2L, "Dra. Sarah Jenkins", "sarah@qrvet.test", "hash", Role.VETERINARIO, true, 0, true);
        when(internacaoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(internacao));
        when(currentUser.getCurrent()).thenReturn(user);
        when(medicacaoRepository.saveAndFlush(any(RegistroMedicacao.class))).thenAnswer(invocation -> {
            RegistroMedicacao r = invocation.getArgument(0);
            r.setId(10L);
            return r;
        });

        var response = service.registerMedication(1L, new RegistroMedicacaoRequest(
                " Ondansetrona ",
                " 0,5 ",
                " mg/kg ",
                " Intravenosa ",
                " Administrar a cada 8 horas ",
                " Sem intercorrências "));

        assertEquals(10L, response.id());
        assertEquals(1L, response.internacaoId());
        assertEquals(2L, response.usuarioId());
        assertEquals("Dra. Sarah Jenkins", response.responsavelNome());
        assertEquals("Ondansetrona", response.medicamento());
        assertEquals("0,5", response.dose());
        assertEquals("mg/kg", response.unidade());
        assertEquals("Intravenosa", response.via());
        assertEquals("Administrar a cada 8 horas", response.prescricao());
        assertEquals("Sem intercorrências", response.observacoes());
    }

    @Test
    void rejectsMedicationWhenHospitalizationNotFoundOrClosed() {
        when(internacaoRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.registerMedication(99L, new RegistroMedicacaoRequest(
                        "Ondansetrona", "0,5", "mg/kg", "Intravenosa", "A cada 8h", null)));

        Internacao closed = Internacao.builder()
                .motivo("Observação")
                .diagnosticoInicial("Avaliação")
                .build();
        closed.close(InternacaoStatus.ALTA);
        when(internacaoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(closed));
        assertThrows(InvalidRequestException.class,
                () -> service.registerMedication(1L, new RegistroMedicacaoRequest(
                        "Ondansetrona", "0,5", "mg/kg", "Intravenosa", "A cada 8h", " ")));
    }

    @Test
    void returnsMedicationHistoryAndChecksHospitalizationExistence() {
        when(internacaoRepository.existsById(99L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.medicationHistory(99L));

        Internacao internacao = Internacao.builder().motivo("Motivo").diagnosticoInicial("Diag").build();
        internacao.setId(1L);
        User user = new User(2L, "Dra. Sarah Jenkins", "sarah@qrvet.test", "hash", Role.VETERINARIO, true, 0, true);
        RegistroMedicacao item = RegistroMedicacao.builder()
                .internacao(internacao)
                .usuario(user)
                .medicamento("Ondansetrona")
                .dose("0,5")
                .unidade("mg/kg")
                .via("Intravenosa")
                .prescricao("A cada 8h")
                .build();
        item.setId(5L);
        when(internacaoRepository.existsById(1L)).thenReturn(true);
        when(medicacaoRepository.findAllByInternacaoIdOrderByDataHoraRegistroDesc(1L))
                .thenReturn(List.of(item));

        var history = service.medicationHistory(1L);
        assertEquals(1, history.size());
        assertEquals("Ondansetrona", history.getFirst().medicamento());
    }
}
