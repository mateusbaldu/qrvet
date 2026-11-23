package ipiranga.fatec.qrvet.dtos.response;

import ipiranga.fatec.qrvet.models.enums.BaiaStatus;
import ipiranga.fatec.qrvet.models.enums.Role;
import java.time.Instant;
import java.util.List;

public record RelatorioClinicoResponse(
        PeriodoRelatorioResponse periodo,
        ResumoInternacoesResponse resumoInternacoes,
        ResumoCuidadosResponse resumoCuidados,
        List<VeterinarioRelatorioResponse> atendimentosPorVeterinario,
        List<ProdutividadeEquipeResponse> produtividadeEquipe,
        List<BaiaRelatorioResponse> utilizacaoBaias) {

    public record PeriodoRelatorioResponse(
            Instant startDate,
            Instant endDate,
            Long veterinarioId) {
    }

    public record ResumoInternacoesResponse(
            long totalNoPeriodo,
            long ativas,
            long altas,
            long obitos,
            long encerradasNoPeriodo,
            double tempoMedioInternacaoHoras) {
    }

    public record ResumoCuidadosResponse(
            long evolucoesRegistradas,
            long medicacoesAplicadas,
            long alimentacoesRegistradas,
            long refeicoesAgendadas,
            long refeicoesConcluidas,
            long refeicoesPendentes,
            long refeicoesCanceladas,
            long jejunsIniciados) {
    }

    public record VeterinarioRelatorioResponse(
            Long veterinarioId,
            String veterinarioNome,
            String veterinarioEmail,
            long totalInternacoes,
            long ativas,
            long altas,
            long obitos,
            long evolucoesRegistradas) {
    }

    public record ProdutividadeEquipeResponse(
            Long usuarioId,
            String nome,
            String email,
            Role role,
            long evolucoesRegistradas,
            long medicacoesAplicadas,
            long alimentacoesRegistradas,
            long totalAcoesClinicas) {
    }

    public record BaiaRelatorioResponse(
            Long baiaId,
            String identificacao,
            BaiaStatus statusAtual,
            long totalInternacoesNoPeriodo) {
    }
}
