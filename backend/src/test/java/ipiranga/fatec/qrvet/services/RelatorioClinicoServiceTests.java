package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.filter.RelatorioFilter;
import ipiranga.fatec.qrvet.dtos.response.RelatorioClinicoResponse;
import ipiranga.fatec.qrvet.exceptions.InvalidRequestException;
import ipiranga.fatec.qrvet.models.AlimentacaoAgendada;
import ipiranga.fatec.qrvet.models.Baia;
import ipiranga.fatec.qrvet.models.Evolucao;
import ipiranga.fatec.qrvet.models.Internacao;
import ipiranga.fatec.qrvet.models.RegistroAlimentacao;
import ipiranga.fatec.qrvet.models.RegistroMedicacao;
import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.models.enums.BaiaStatus;
import ipiranga.fatec.qrvet.models.enums.InternacaoStatus;
import ipiranga.fatec.qrvet.models.enums.Role;
import ipiranga.fatec.qrvet.repositories.AlimentacaoAgendadaRepository;
import ipiranga.fatec.qrvet.repositories.BaiaRepository;
import ipiranga.fatec.qrvet.repositories.EvolucaoRepository;
import ipiranga.fatec.qrvet.repositories.InternacaoRepository;
import ipiranga.fatec.qrvet.repositories.JejumRepository;
import ipiranga.fatec.qrvet.repositories.RegistroAlimentacaoRepository;
import ipiranga.fatec.qrvet.repositories.RegistroMedicacaoRepository;
import ipiranga.fatec.qrvet.repositories.UserRepository;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RelatorioClinicoServiceTests {
    private final InternacaoRepository internacaoRepository = mock(InternacaoRepository.class);
    private final EvolucaoRepository evolucaoRepository = mock(EvolucaoRepository.class);
    private final RegistroMedicacaoRepository medicacaoRepository = mock(RegistroMedicacaoRepository.class);
    private final RegistroAlimentacaoRepository alimentacaoRepository = mock(RegistroAlimentacaoRepository.class);
    private final AlimentacaoAgendadaRepository agendaAlimentacaoRepository = mock(AlimentacaoAgendadaRepository.class);
    private final JejumRepository jejumRepository = mock(JejumRepository.class);
    private final BaiaRepository baiaRepository = mock(BaiaRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    private final RelatorioClinicoService service = new RelatorioClinicoService(
            internacaoRepository,
            evolucaoRepository,
            medicacaoRepository,
            alimentacaoRepository,
            agendaAlimentacaoRepository,
            jejumRepository,
            baiaRepository,
            userRepository);

    @Test
    void rejectsEndDateBeforeStartDate() {
        Instant start = Instant.parse("2026-05-10T00:00:00Z");
        Instant end = Instant.parse("2026-05-01T00:00:00Z");

        assertThrows(InvalidRequestException.class,
                () -> service.gerarRelatorio(new RelatorioFilter(start, end, null)));
    }

    @Test
    void aggregatesVeterinarianRankingCareProductivityBayUtilizationAndExportsCsv() {
        User vet1 = new User(10L, "Dra. Camila", "camila@qrvet.test", "hash", Role.VETERINARIO, true, 0L, true);
        User vet2 = new User(11L, "Dr. Rafael", "rafael@qrvet.test", "hash", Role.VETERINARIO, true, 0L, true);
        User vet3Zero = new User(12L, "Dr. Bruno", "bruno@qrvet.test", "hash", Role.VETERINARIO, true, 0L, true);
        User aux = new User(20L, "=Auxiliar Joana", "joana@qrvet.test", "hash", Role.AUXILIAR_TECNICO, true, 0L, true);

        Baia baia1 = Baia.builder().identificacao("B-01").status(BaiaStatus.OCUPADA).build();
        setField(baia1, "id", 100L);
        Baia baia2 = Baia.builder().identificacao("B-02").status(BaiaStatus.DISPONIVEL).build();
        setField(baia2, "id", 101L);

        Internacao i1 = Internacao.builder().veterinario(vet1).baia(baia1).motivo("M1").diagnosticoInicial("D1").build();
        i1.setId(1L);
        i1.setStatus(InternacaoStatus.ALTA);
        i1.setEntradaInternacao(Instant.parse("2026-05-01T10:00:00Z"));
        i1.setSaidaInternacao(Instant.parse("2026-05-02T10:00:00Z")); // 24h

        Internacao i2 = Internacao.builder().veterinario(vet1).baia(baia1).motivo("M2").diagnosticoInicial("D2").build();
        i2.setId(2L);
        i2.setStatus(InternacaoStatus.ATIVA);
        i2.setEntradaInternacao(Instant.parse("2026-05-03T10:00:00Z"));

        Internacao i3 = Internacao.builder().veterinario(vet2).baia(baia2).motivo("M3").diagnosticoInicial("D3").build();
        i3.setId(3L);
        i3.setStatus(InternacaoStatus.OBITO);
        i3.setEntradaInternacao(Instant.parse("2026-05-04T00:00:00Z"));
        i3.setSaidaInternacao(Instant.parse("2026-05-04T12:00:00Z")); // 12h

        Evolucao ev1 = new Evolucao(i1, vet1, "Evolução 1");
        Evolucao ev2 = new Evolucao(i2, aux, "Evolução 2");

        RegistroMedicacao med1 = RegistroMedicacao.builder()
                .internacao(i1).usuario(aux).medicamento("Dipirona").dose("1").unidade("mL").via("IV").prescricao("8/8h").build();
        RegistroAlimentacao alim1 = RegistroAlimentacao.builder()
                .internacao(i1).usuario(aux).alimento("Ração").quantidade("50g").build();

        AlimentacaoAgendada ag1 = new AlimentacaoAgendada(i1, "Ração", "50g", Instant.parse("2026-05-01T12:00:00Z"), vet1);
        ag1.complete(alim1);
        AlimentacaoAgendada ag2 = new AlimentacaoAgendada(i2, "Ração", "50g", Instant.parse("2026-05-03T12:00:00Z"), vet1);
        AlimentacaoAgendada ag3 = new AlimentacaoAgendada(i2, "Ração", "50g", Instant.parse("2026-05-03T18:00:00Z"), vet1);
        ag3.cancel("Jejum");

        when(userRepository.findAllByRoleAndActiveTrueAndConfirmedTrueOrderByNameAsc(Role.VETERINARIO))
                .thenReturn(List.of(vet3Zero, vet1, vet2));
        when(baiaRepository.findAll()).thenReturn(List.of(baia1, baia2));
        when(internacaoRepository.findForReport(any(), any(), any())).thenReturn(List.of(i1, i2, i3));
        when(internacaoRepository.countClosedInPeriod(any(), any(), any())).thenReturn(2L);
        when(evolucaoRepository.findForReport(any(), any(), any())).thenReturn(List.of(ev1, ev2));
        when(medicacaoRepository.findForReport(any(), any(), any())).thenReturn(List.of(med1));
        when(alimentacaoRepository.findForReport(any(), any(), any())).thenReturn(List.of(alim1));
        when(agendaAlimentacaoRepository.findForReport(any(), any(), any())).thenReturn(List.of(ag1, ag2, ag3));
        when(jejumRepository.countForReport(any(), any(), any())).thenReturn(1L);

        Instant start = Instant.parse("2026-05-01T00:00:00Z");
        Instant end = Instant.parse("2026-05-31T23:59:59Z");
        RelatorioFilter filter = new RelatorioFilter(start, end, null);

        RelatorioClinicoResponse report = service.gerarRelatorio(filter);

        assertEquals(3L, report.resumoInternacoes().totalNoPeriodo());
        assertEquals(1L, report.resumoInternacoes().ativas());
        assertEquals(1L, report.resumoInternacoes().altas());
        assertEquals(1L, report.resumoInternacoes().obitos());
        assertEquals(2L, report.resumoInternacoes().encerradasNoPeriodo());
        assertEquals(18.0, report.resumoInternacoes().tempoMedioInternacaoHoras());

        assertEquals(2L, report.resumoCuidados().evolucoesRegistradas());
        assertEquals(1L, report.resumoCuidados().medicacoesAplicadas());
        assertEquals(1L, report.resumoCuidados().alimentacoesRegistradas());
        assertEquals(3L, report.resumoCuidados().refeicoesAgendadas());
        assertEquals(1L, report.resumoCuidados().refeicoesConcluidas());
        assertEquals(1L, report.resumoCuidados().refeicoesPendentes());
        assertEquals(1L, report.resumoCuidados().refeicoesCanceladas());
        assertEquals(1L, report.resumoCuidados().jejunsIniciados());

        assertEquals(3, report.atendimentosPorVeterinario().size());
        assertEquals("Dra. Camila", report.atendimentosPorVeterinario().get(0).veterinarioNome());
        assertEquals(2L, report.atendimentosPorVeterinario().get(0).totalInternacoes());
        assertEquals(1L, report.atendimentosPorVeterinario().get(0).ativas());
        assertEquals(1L, report.atendimentosPorVeterinario().get(0).altas());
        assertEquals(1L, report.atendimentosPorVeterinario().get(0).evolucoesRegistradas());

        assertEquals("Dr. Rafael", report.atendimentosPorVeterinario().get(1).veterinarioNome());
        assertEquals(1L, report.atendimentosPorVeterinario().get(1).totalInternacoes());
        assertEquals(1L, report.atendimentosPorVeterinario().get(1).obitos());

        assertEquals("Dr. Bruno", report.atendimentosPorVeterinario().get(2).veterinarioNome());
        assertEquals(0L, report.atendimentosPorVeterinario().get(2).totalInternacoes());

        assertEquals(2, report.produtividadeEquipe().size());
        assertEquals("=Auxiliar Joana", report.produtividadeEquipe().get(0).nome());
        assertEquals(3L, report.produtividadeEquipe().get(0).totalAcoesClinicas());

        assertEquals(2, report.utilizacaoBaias().size());
        assertEquals("B-01", report.utilizacaoBaias().get(0).identificacao());
        assertEquals(2L, report.utilizacaoBaias().get(0).totalInternacoesNoPeriodo());

        String csv = new String(service.exportarCsv(filter), StandardCharsets.UTF_8);
        assertTrue(csv.contains("RESUMO DE INTERNAÇÕES"));
        assertTrue(csv.contains("\"Tempo médio de internação (horas)\",18.00"));
        assertTrue(csv.contains("ATENDIMENTOS POR VETERINÁRIO"));
        assertTrue(csv.contains("10,\"Dra. Camila\",\"camila@qrvet.test\",2,1,1,0,1"));
        assertTrue(csv.contains("PRODUTIVIDADE DA EQUIPE"));
        assertTrue(csv.contains("\"'=Auxiliar Joana\""));
        assertTrue(csv.contains("UTILIZAÇÃO DE BAIAS"));
        assertTrue(csv.contains("100,\"B-01\",\"OCUPADA\",2"));
    }

    private static void setField(Object target, String name, Object value) {
        try {
            var field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
