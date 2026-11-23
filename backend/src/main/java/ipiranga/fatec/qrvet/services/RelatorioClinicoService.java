package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.filter.RelatorioFilter;
import ipiranga.fatec.qrvet.dtos.response.RelatorioClinicoResponse;
import ipiranga.fatec.qrvet.dtos.response.RelatorioClinicoResponse.BaiaRelatorioResponse;
import ipiranga.fatec.qrvet.dtos.response.RelatorioClinicoResponse.PeriodoRelatorioResponse;
import ipiranga.fatec.qrvet.dtos.response.RelatorioClinicoResponse.ProdutividadeEquipeResponse;
import ipiranga.fatec.qrvet.dtos.response.RelatorioClinicoResponse.ResumoCuidadosResponse;
import ipiranga.fatec.qrvet.dtos.response.RelatorioClinicoResponse.ResumoInternacoesResponse;
import ipiranga.fatec.qrvet.dtos.response.RelatorioClinicoResponse.VeterinarioRelatorioResponse;
import ipiranga.fatec.qrvet.exceptions.InvalidRequestException;
import ipiranga.fatec.qrvet.models.AlimentacaoAgendada;
import ipiranga.fatec.qrvet.models.Baia;
import ipiranga.fatec.qrvet.models.Evolucao;
import ipiranga.fatec.qrvet.models.Internacao;
import ipiranga.fatec.qrvet.models.RegistroAlimentacao;
import ipiranga.fatec.qrvet.models.RegistroMedicacao;
import ipiranga.fatec.qrvet.models.User;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class RelatorioClinicoService {
    private final InternacaoRepository internacaoRepository;
    private final EvolucaoRepository evolucaoRepository;
    private final RegistroMedicacaoRepository medicacaoRepository;
    private final RegistroAlimentacaoRepository alimentacaoRepository;
    private final AlimentacaoAgendadaRepository agendaAlimentacaoRepository;
    private final JejumRepository jejumRepository;
    private final BaiaRepository baiaRepository;
    private final UserRepository userRepository;

    public RelatorioClinicoService(
            InternacaoRepository internacaoRepository,
            EvolucaoRepository evolucaoRepository,
            RegistroMedicacaoRepository medicacaoRepository,
            RegistroAlimentacaoRepository alimentacaoRepository,
            AlimentacaoAgendadaRepository agendaAlimentacaoRepository,
            JejumRepository jejumRepository,
            BaiaRepository baiaRepository,
            UserRepository userRepository) {
        this.internacaoRepository = internacaoRepository;
        this.evolucaoRepository = evolucaoRepository;
        this.medicacaoRepository = medicacaoRepository;
        this.alimentacaoRepository = alimentacaoRepository;
        this.agendaAlimentacaoRepository = agendaAlimentacaoRepository;
        this.jejumRepository = jejumRepository;
        this.baiaRepository = baiaRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public RelatorioClinicoResponse gerarRelatorio(RelatorioFilter filter) {
        RelatorioFilter safe = normalizeAndValidate(filter);

        List<Internacao> internacoes = internacaoRepository.findForReport(
                safe.startDate(), safe.endDate(), safe.veterinarioId());
        long encerradasNoPeriodo = internacaoRepository.countClosedInPeriod(
                safe.startDate(), safe.endDate(), safe.veterinarioId());
        List<Evolucao> evolucoes = evolucaoRepository.findForReport(
                safe.startDate(), safe.endDate(), safe.veterinarioId());
        List<RegistroMedicacao> medicacoes = medicacaoRepository.findForReport(
                safe.startDate(), safe.endDate(), safe.veterinarioId());
        List<RegistroAlimentacao> alimentacoes = alimentacaoRepository.findForReport(
                safe.startDate(), safe.endDate(), safe.veterinarioId());
        List<AlimentacaoAgendada> agendas = agendaAlimentacaoRepository.findForReport(
                safe.startDate(), safe.endDate(), safe.veterinarioId());
        long jejunsIniciados = jejumRepository.countForReport(
                safe.startDate(), safe.endDate(), safe.veterinarioId());

        ResumoInternacoesResponse resumoInternacoes = buildResumoInternacoes(internacoes, encerradasNoPeriodo);
        ResumoCuidadosResponse resumoCuidados = buildResumoCuidados(
                evolucoes, medicacoes, alimentacoes, agendas, jejunsIniciados);
        List<VeterinarioRelatorioResponse> porVeterinario = buildAtendimentosPorVeterinario(
                internacoes, evolucoes, safe.veterinarioId());
        List<ProdutividadeEquipeResponse> produtividade = buildProdutividadeEquipe(
                evolucoes, medicacoes, alimentacoes);
        List<BaiaRelatorioResponse> baias = buildUtilizacaoBaias(internacoes);

        return new RelatorioClinicoResponse(
                new PeriodoRelatorioResponse(safe.startDate(), safe.endDate(), safe.veterinarioId()),
                resumoInternacoes,
                resumoCuidados,
                porVeterinario,
                produtividade,
                baias);
    }

    @Transactional(readOnly = true)
    public byte[] exportarCsv(RelatorioFilter filter) {
        RelatorioClinicoResponse report = gerarRelatorio(filter);

        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');

        csv.append("PERÍODO DO RELATÓRIO\r\n");
        csv.append("Data Inicial (UTC),Data Final (UTC),ID Veterinário Filtrado\r\n");
        csv.append(csvCell(instantOrEmpty(report.periodo().startDate()))).append(',')
                .append(csvCell(instantOrEmpty(report.periodo().endDate()))).append(',')
                .append(numberOrEmpty(report.periodo().veterinarioId())).append("\r\n\r\n");

        ResumoInternacoesResponse ri = report.resumoInternacoes();
        csv.append("RESUMO DE INTERNAÇÕES\r\n");
        csv.append("Indicador,Valor\r\n");
        csv.append(csvCell("Total de internações abertas no período")).append(',').append(ri.totalNoPeriodo()).append("\r\n");
        csv.append(csvCell("Internações ativas")).append(',').append(ri.ativas()).append("\r\n");
        csv.append(csvCell("Internações com alta")).append(',').append(ri.altas()).append("\r\n");
        csv.append(csvCell("Internações com óbito")).append(',').append(ri.obitos()).append("\r\n");
        csv.append(csvCell("Internações encerradas no período")).append(',').append(ri.encerradasNoPeriodo()).append("\r\n");
        csv.append(csvCell("Tempo médio de internação (horas)")).append(',')
                .append(String.format(Locale.US, "%.2f", ri.tempoMedioInternacaoHoras())).append("\r\n\r\n");

        ResumoCuidadosResponse rc = report.resumoCuidados();
        csv.append("RESUMO DE CUIDADOS CLÍNICOS\r\n");
        csv.append("Indicador,Total\r\n");
        csv.append(csvCell("Evoluções registradas")).append(',').append(rc.evolucoesRegistradas()).append("\r\n");
        csv.append(csvCell("Medicações aplicadas")).append(',').append(rc.medicacoesAplicadas()).append("\r\n");
        csv.append(csvCell("Alimentações registradas")).append(',').append(rc.alimentacoesRegistradas()).append("\r\n");
        csv.append(csvCell("Refeições agendadas")).append(',').append(rc.refeicoesAgendadas()).append("\r\n");
        csv.append(csvCell("Refeições concluídas")).append(',').append(rc.refeicoesConcluidas()).append("\r\n");
        csv.append(csvCell("Refeições pendentes")).append(',').append(rc.refeicoesPendentes()).append("\r\n");
        csv.append(csvCell("Refeições canceladas")).append(',').append(rc.refeicoesCanceladas()).append("\r\n");
        csv.append(csvCell("Jejuns iniciados")).append(',').append(rc.jejunsIniciados()).append("\r\n\r\n");

        csv.append("ATENDIMENTOS POR VETERINÁRIO\r\n");
        csv.append("ID Veterinário,Nome,E-mail,Total Internações,Ativas,Altas,Óbitos,Evoluções Registradas\r\n");
        for (VeterinarioRelatorioResponse vet : report.atendimentosPorVeterinario()) {
            csv.append(numberOrEmpty(vet.veterinarioId())).append(',')
                    .append(csvCell(vet.veterinarioNome())).append(',')
                    .append(csvCell(vet.veterinarioEmail())).append(',')
                    .append(vet.totalInternacoes()).append(',')
                    .append(vet.ativas()).append(',')
                    .append(vet.altas()).append(',')
                    .append(vet.obitos()).append(',')
                    .append(vet.evolucoesRegistradas()).append("\r\n");
        }
        csv.append("\r\n");

        csv.append("PRODUTIVIDADE DA EQUIPE\r\n");
        csv.append("ID Usuário,Nome,E-mail,Perfil,Evoluções Registradas,Medicações Aplicadas,Alimentações Registradas,Total Ações Clínicas\r\n");
        for (ProdutividadeEquipeResponse member : report.produtividadeEquipe()) {
            csv.append(numberOrEmpty(member.usuarioId())).append(',')
                    .append(csvCell(member.nome())).append(',')
                    .append(csvCell(member.email())).append(',')
                    .append(csvCell(member.role() != null ? member.role().name() : "")).append(',')
                    .append(member.evolucoesRegistradas()).append(',')
                    .append(member.medicacoesAplicadas()).append(',')
                    .append(member.alimentacoesRegistradas()).append(',')
                    .append(member.totalAcoesClinicas()).append("\r\n");
        }
        csv.append("\r\n");

        csv.append("UTILIZAÇÃO DE BAIAS\r\n");
        csv.append("ID Baia,Identificação,Status Atual,Internações no Período\r\n");
        for (BaiaRelatorioResponse bay : report.utilizacaoBaias()) {
            csv.append(numberOrEmpty(bay.baiaId())).append(',')
                    .append(csvCell(bay.identificacao())).append(',')
                    .append(csvCell(bay.statusAtual() != null ? bay.statusAtual().name() : "")).append(',')
                    .append(bay.totalInternacoesNoPeriodo()).append("\r\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private RelatorioFilter normalizeAndValidate(RelatorioFilter filter) {
        if (filter == null) {
            return new RelatorioFilter(null, null, null);
        }
        if (filter.startDate() != null && filter.endDate() != null
                && filter.endDate().isBefore(filter.startDate())) {
            throw new InvalidRequestException("The end date must be equal to or after the start date.");
        }
        return filter;
    }

    private ResumoInternacoesResponse buildResumoInternacoes(
            List<Internacao> internacoes,
            long encerradasNoPeriodo) {
        long total = internacoes.size();
        long ativas = 0;
        long altas = 0;
        long obitos = 0;
        double somaHorasEncerradas = 0.0;
        long quantidadeComSaida = 0;

        for (Internacao internacao : internacoes) {
            if (internacao.getStatus() == InternacaoStatus.ATIVA) {
                ativas++;
            } else if (internacao.getStatus() == InternacaoStatus.ALTA) {
                altas++;
            } else if (internacao.getStatus() == InternacaoStatus.OBITO) {
                obitos++;
            }
            if (internacao.getEntradaInternacao() != null && internacao.getSaidaInternacao() != null
                    && !internacao.getSaidaInternacao().isBefore(internacao.getEntradaInternacao())) {
                double horas = Duration.between(internacao.getEntradaInternacao(), internacao.getSaidaInternacao())
                        .toMillis() / 3_600_000.0;
                somaHorasEncerradas += horas;
                quantidadeComSaida++;
            }
        }

        double tempoMedioHoras = 0.0;
        if (quantidadeComSaida > 0) {
            tempoMedioHoras = BigDecimal.valueOf(somaHorasEncerradas / quantidadeComSaida)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return new ResumoInternacoesResponse(
                total, ativas, altas, obitos, encerradasNoPeriodo, tempoMedioHoras);
    }

    private ResumoCuidadosResponse buildResumoCuidados(
            List<Evolucao> evolucoes,
            List<RegistroMedicacao> medicacoes,
            List<RegistroAlimentacao> alimentacoes,
            List<AlimentacaoAgendada> agendas,
            long jejunsIniciados) {
        long concluidas = 0;
        long pendentes = 0;
        long canceladas = 0;
        for (AlimentacaoAgendada agenda : agendas) {
            if (agenda.getStatus() == AlimentacaoAgendada.Status.CONCLUIDA) {
                concluidas++;
            } else if (agenda.getStatus() == AlimentacaoAgendada.Status.PENDENTE) {
                pendentes++;
            } else if (agenda.getStatus() == AlimentacaoAgendada.Status.CANCELADA) {
                canceladas++;
            }
        }
        return new ResumoCuidadosResponse(
                evolucoes.size(),
                medicacoes.size(),
                alimentacoes.size(),
                agendas.size(),
                concluidas,
                pendentes,
                canceladas,
                jejunsIniciados);
    }

    private List<VeterinarioRelatorioResponse> buildAtendimentosPorVeterinario(
            List<Internacao> internacoes,
            List<Evolucao> evolucoes,
            Long filteredVeterinarioId) {
        Map<Long, VetAccumulator> accumulators = new LinkedHashMap<>();

        List<User> activeVets = userRepository.findAllByRoleAndActiveTrueAndConfirmedTrueOrderByNameAsc(Role.VETERINARIO);
        for (User vet : activeVets) {
            if (filteredVeterinarioId == null || filteredVeterinarioId.equals(vet.getId())) {
                accumulators.put(vet.getId(), new VetAccumulator(vet));
            }
        }

        for (Internacao internacao : internacoes) {
            User vet = internacao.getVeterinario();
            if (vet == null) {
                continue;
            }
            VetAccumulator acc = accumulators.computeIfAbsent(vet.getId(), id -> new VetAccumulator(vet));
            acc.totalInternacoes++;
            if (internacao.getStatus() == InternacaoStatus.ATIVA) {
                acc.ativas++;
            } else if (internacao.getStatus() == InternacaoStatus.ALTA) {
                acc.altas++;
            } else if (internacao.getStatus() == InternacaoStatus.OBITO) {
                acc.obitos++;
            }
        }

        for (Evolucao evolucao : evolucoes) {
            User author = evolucao.getUsuario();
            if (author == null) {
                continue;
            }
            VetAccumulator acc = accumulators.get(author.getId());
            if (acc != null) {
                acc.evolucoesRegistradas++;
            } else if (author.getRole() == Role.VETERINARIO
                    && (filteredVeterinarioId == null || filteredVeterinarioId.equals(author.getId()))) {
                VetAccumulator created = new VetAccumulator(author);
                created.evolucoesRegistradas++;
                accumulators.put(author.getId(), created);
            }
        }

        return accumulators.values().stream()
                .map(VetAccumulator::toResponse)
                .sorted(Comparator
                        .comparingLong(VeterinarioRelatorioResponse::totalInternacoes).reversed()
                        .thenComparing(Comparator.comparingLong(VeterinarioRelatorioResponse::evolucoesRegistradas).reversed())
                        .thenComparing(v -> v.veterinarioNome() != null ? v.veterinarioNome().toLowerCase(Locale.ROOT) : ""))
                .toList();
    }

    private List<ProdutividadeEquipeResponse> buildProdutividadeEquipe(
            List<Evolucao> evolucoes,
            List<RegistroMedicacao> medicacoes,
            List<RegistroAlimentacao> alimentacoes) {
        Map<Long, TeamAccumulator> accumulators = new LinkedHashMap<>();

        for (Evolucao evolucao : evolucoes) {
            User user = evolucao.getUsuario();
            if (user != null) {
                accumulators.computeIfAbsent(user.getId(), id -> new TeamAccumulator(user)).evolucoes++;
            }
        }
        for (RegistroMedicacao medicacao : medicacoes) {
            User user = medicacao.getUsuario();
            if (user != null) {
                accumulators.computeIfAbsent(user.getId(), id -> new TeamAccumulator(user)).medicacoes++;
            }
        }
        for (RegistroAlimentacao alimentacao : alimentacoes) {
            User user = alimentacao.getUsuario();
            if (user != null) {
                accumulators.computeIfAbsent(user.getId(), id -> new TeamAccumulator(user)).alimentacoes++;
            }
        }

        return accumulators.values().stream()
                .map(TeamAccumulator::toResponse)
                .sorted(Comparator
                        .comparingLong(ProdutividadeEquipeResponse::totalAcoesClinicas).reversed()
                        .thenComparing(m -> m.nome() != null ? m.nome().toLowerCase(Locale.ROOT) : ""))
                .toList();
    }

    private List<BaiaRelatorioResponse> buildUtilizacaoBaias(List<Internacao> internacoes) {
        Map<Long, BayAccumulator> accumulators = new LinkedHashMap<>();

        for (Baia baia : baiaRepository.findAll()) {
            accumulators.put(baia.getId(), new BayAccumulator(baia));
        }

        for (Internacao internacao : internacoes) {
            Baia baia = internacao.getBaia();
            if (baia != null) {
                accumulators.computeIfAbsent(baia.getId(), id -> new BayAccumulator(baia)).totalInternacoes++;
            }
        }

        return accumulators.values().stream()
                .map(BayAccumulator::toResponse)
                .sorted(Comparator
                        .comparingLong(BaiaRelatorioResponse::totalInternacoesNoPeriodo).reversed()
                        .thenComparing(b -> b.identificacao() != null ? b.identificacao().toLowerCase(Locale.ROOT) : ""))
                .toList();
    }

    private String numberOrEmpty(Long value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    private String instantOrEmpty(Instant value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    private String csvCell(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String sanitized = value;
        char first = sanitized.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@') {
            sanitized = "'" + sanitized;
        }
        return "\"" + sanitized.replace("\"", "\"\"") + "\"";
    }

    private static final class VetAccumulator {
        private final Long id;
        private final String nome;
        private final String email;
        private long totalInternacoes;
        private long ativas;
        private long altas;
        private long obitos;
        private long evolucoesRegistradas;

        private VetAccumulator(User user) {
            this.id = user.getId();
            this.nome = user.getName();
            this.email = user.getEmail();
        }

        private VeterinarioRelatorioResponse toResponse() {
            return new VeterinarioRelatorioResponse(
                    id, nome, email, totalInternacoes, ativas, altas, obitos, evolucoesRegistradas);
        }
    }

    private static final class TeamAccumulator {
        private final Long id;
        private final String nome;
        private final String email;
        private final Role role;
        private long evolucoes;
        private long medicacoes;
        private long alimentacoes;

        private TeamAccumulator(User user) {
            this.id = user.getId();
            this.nome = user.getName();
            this.email = user.getEmail();
            this.role = user.getRole();
        }

        private ProdutividadeEquipeResponse toResponse() {
            long total = evolucoes + medicacoes + alimentacoes;
            return new ProdutividadeEquipeResponse(
                    id, nome, email, role, evolucoes, medicacoes, alimentacoes, total);
        }
    }

    private static final class BayAccumulator {
        private final Long id;
        private final String identificacao;
        private final ipiranga.fatec.qrvet.models.enums.BaiaStatus statusAtual;
        private long totalInternacoes;

        private BayAccumulator(Baia baia) {
            this.id = baia.getId();
            this.identificacao = baia.getIdentificacao();
            this.statusAtual = baia.getStatus();
        }

        private BaiaRelatorioResponse toResponse() {
            return new BaiaRelatorioResponse(id, identificacao, statusAtual, totalInternacoes);
        }
    }
}
