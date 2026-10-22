package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.request.AgendaAlimentacaoRequest;
import ipiranga.fatec.qrvet.dtos.request.JejumRequest;
import ipiranga.fatec.qrvet.dtos.request.RegistroAlimentacaoRequest;
import ipiranga.fatec.qrvet.dtos.response.AgendaAlimentacaoResponse;
import ipiranga.fatec.qrvet.dtos.response.JejumResponse;
import ipiranga.fatec.qrvet.dtos.response.RegistroAlimentacaoResponse;
import ipiranga.fatec.qrvet.exceptions.InvalidRequestException;
import ipiranga.fatec.qrvet.exceptions.OperationConflictException;
import ipiranga.fatec.qrvet.models.*;
import ipiranga.fatec.qrvet.models.enums.InternacaoStatus;
import ipiranga.fatec.qrvet.models.enums.Role;
import ipiranga.fatec.qrvet.repositories.AlimentacaoAgendadaRepository;
import ipiranga.fatec.qrvet.repositories.InternacaoRepository;
import ipiranga.fatec.qrvet.repositories.JejumRepository;
import ipiranga.fatec.qrvet.repositories.RegistroAlimentacaoRepository;
import ipiranga.fatec.qrvet.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NutricaoClinicaServiceTests {
    private final AlimentacaoAgendadaRepository agendaRepository = mock(AlimentacaoAgendadaRepository.class);
    private final RegistroAlimentacaoRepository registroRepository = mock(RegistroAlimentacaoRepository.class);
    private final JejumRepository jejumRepository = mock(JejumRepository.class);
    private final InternacaoRepository internacaoRepository = mock(InternacaoRepository.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);

    private final NutricaoClinicaService service = new NutricaoClinicaService(
            agendaRepository,
            registroRepository,
            jejumRepository,
            internacaoRepository,
            currentUser);

    private Internacao internacao;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User(1L, "Veterinario", "vet@test.com", "hash", Role.VETERINARIO, true, 0, true);
        when(currentUser.getCurrent()).thenReturn(user);

        Paciente paciente = mock(Paciente.class);
        when(paciente.getNome()).thenReturn("Rex");

        Baia baia = mock(Baia.class);
        when(baia.getIdentificacao()).thenReturn("Baia 01");

        internacao = mock(Internacao.class);
        when(internacao.getId()).thenReturn(10L);
        when(internacao.getStatus()).thenReturn(InternacaoStatus.ATIVA);
        when(internacao.getPaciente()).thenReturn(paciente);
        when(internacao.getBaia()).thenReturn(baia);
        when(internacaoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(internacao));
    }

    @Test
    void createsScheduleSuccessfully() {
        Instant future1 = Instant.now().plus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        Instant future2 = Instant.now().plus(4, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        var request = new AgendaAlimentacaoRequest("Ração Úmida", "50g", List.of(future1, future2));

        when(agendaRepository.existsByInternacaoIdAndHorarioAndStatusNot(eq(10L), any(), any())).thenReturn(false);
        when(jejumRepository.existsByInternacaoIdAndAtivoTrue(10L)).thenReturn(false);
        when(agendaRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<AgendaAlimentacaoResponse> responses = service.createSchedule(10L, request);

        assertEquals(2, responses.size());
        assertEquals("Ração Úmida", responses.get(0).alimento());
        assertFalse(responses.get(0).jejumAtivo());
    }

    @Test
    void rejectsPastScheduleTime() {
        Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
        var request = new AgendaAlimentacaoRequest("Ração", "30g", List.of(past));

        assertThrows(InvalidRequestException.class, () -> service.createSchedule(10L, request));
    }

    @Test
    void completesScheduleAndGeneratesRecordAtomically() {
        Instant past = Instant.now().minus(5, ChronoUnit.MINUTES);
        AlimentacaoAgendada agendada = new AlimentacaoAgendada(internacao, "Sachê", "85g", past, user);

        when(agendaRepository.findById(100L)).thenReturn(Optional.of(agendada));
        when(jejumRepository.existsByInternacaoIdAndAtivoTrue(10L)).thenReturn(false);
        when(registroRepository.saveAndFlush(any(RegistroAlimentacao.class))).thenAnswer(invocation -> {
            RegistroAlimentacao r = invocation.getArgument(0);
            r.setId(500L);
            return r;
        });

        AgendaAlimentacaoResponse response = service.completeSchedule(10L, 100L, "Comeu tudo com boa aceitação");

        assertEquals(AlimentacaoAgendada.Status.CONCLUIDA, agendada.getStatus());
        assertEquals("CONCLUIDA", response.status());
        verify(registroRepository).saveAndFlush(any(RegistroAlimentacao.class));
    }

    @Test
    void rejectsCompletingScheduleWhileFastingIsActive() {
        Instant past = Instant.now().minus(5, ChronoUnit.MINUTES);
        AlimentacaoAgendada agendada = new AlimentacaoAgendada(internacao, "Sachê", "85g", past, user);

        when(agendaRepository.findById(100L)).thenReturn(Optional.of(agendada));
        when(jejumRepository.existsByInternacaoIdAndAtivoTrue(10L)).thenReturn(true);

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> service.completeSchedule(10L, 100L, "Tentou alimentar"));

        assertTrue(ex.getMessage().contains("active fasting"));
        assertEquals(AlimentacaoAgendada.Status.PENDENTE, agendada.getStatus());
        verify(registroRepository, never()).saveAndFlush(any());
    }

    @Test
    void startsAndEndsFasting() {
        var request = new JejumRequest("Cirurgia amanhã cedo");
        when(jejumRepository.existsByInternacaoIdAndAtivoTrue(10L)).thenReturn(false);
        when(jejumRepository.saveAndFlush(any(Jejum.class))).thenAnswer(invocation -> {
            Jejum j = invocation.getArgument(0);
            return j;
        });

        JejumResponse started = service.startFasting(10L, request);
        assertEquals("Cirurgia amanhã cedo", started.motivo());
        assertTrue(started.ativo());

        Jejum jejumAtivo = spy(Jejum.builder().internacao(internacao).motivo("Cirurgia").build());
        when(jejumRepository.findActiveForUpdate(10L)).thenReturn(Optional.of(jejumAtivo));

        JejumResponse ended = service.endFasting(10L);
        assertFalse(ended.ativo());
        assertNotNull(ended.dataHoraFim());
    }

    @Test
    void rejectsDuplicateActiveFasting() {
        when(jejumRepository.existsByInternacaoIdAndAtivoTrue(10L)).thenReturn(true);

        assertThrows(OperationConflictException.class,
                () -> service.startFasting(10L, new JejumRequest("Outro jejum")));
    }

    @Test
    void registersFoodDirectlyWhenNoFasting() {
        var request = new RegistroAlimentacaoRequest("Ração Renal", "40g", "Boa aceitação");
        when(jejumRepository.existsByInternacaoIdAndAtivoTrue(10L)).thenReturn(false);
        when(registroRepository.saveAndFlush(any(RegistroAlimentacao.class))).thenAnswer(invocation -> {
            RegistroAlimentacao r = invocation.getArgument(0);
            r.setId(99L);
            return r;
        });

        RegistroAlimentacaoResponse response = service.registerFood(10L, request);
        assertEquals("Ração Renal", response.alimento());
        assertEquals("40g", response.quantidade());
    }

    @Test
    void rejectsDirectFoodRegistrationWhenFastingActive() {
        var request = new RegistroAlimentacaoRequest("Ração", "40g", "Tentativa");
        when(jejumRepository.existsByInternacaoIdAndAtivoTrue(10L)).thenReturn(true);

        assertThrows(InvalidRequestException.class, () -> service.registerFood(10L, request));
        verify(registroRepository, never()).saveAndFlush(any());
    }

    @Test
    void cancelsSchedulesAndEndsFastingOnDischarge() {
        AlimentacaoAgendada p1 = new AlimentacaoAgendada(internacao, "Ração", "30g", Instant.now().plusSeconds(3600), user);
        when(agendaRepository.findAllByInternacaoIdAndStatus(10L, AlimentacaoAgendada.Status.PENDENTE))
                .thenReturn(List.of(p1));

        Jejum ativo = spy(Jejum.builder().internacao(internacao).motivo("Cirurgia").build());
        when(jejumRepository.findActiveForUpdate(10L)).thenReturn(Optional.of(ativo));

        service.cancelForDischarge(10L);

        assertEquals(AlimentacaoAgendada.Status.CANCELADA, p1.getStatus());
        assertEquals("Internação encerrada", p1.getMotivoCancelamento());
        assertFalse(ativo.isAtivo());
    }
}
