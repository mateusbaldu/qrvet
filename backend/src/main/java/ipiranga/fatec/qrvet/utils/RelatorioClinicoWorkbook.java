package ipiranga.fatec.qrvet.utils;

import ipiranga.fatec.qrvet.dtos.response.RelatorioClinicoResponse;
import ipiranga.fatec.qrvet.models.enums.BaiaStatus;
import ipiranga.fatec.qrvet.models.enums.Role;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Objects;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class RelatorioClinicoWorkbook {
    private static final XSSFColor VERDE = cor("0D443D");
    private static final XSSFColor TEXTO = cor("0B302D");
    private static final XSSFColor SUAVE = cor("E9F5EE");
    private static final XSSFColor LINHA = cor("C9E2D4");
    private static final XSSFColor NOTA = cor("4F6860");
    private static final XSSFColor BRANCO = cor("FFFFFF");
    private static final int LINHA_CABECALHO = 4;
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    private final XSSFWorkbook workbook;
    private final XSSFCellStyle titulo;
    private final XSSFCellStyle divisoria;
    private final XSSFCellStyle secao;
    private final XSSFCellStyle cabecalho;
    private final XSSFCellStyle contexto;
    private final XSSFCellStyle nota;
    private final XSSFCellStyle data;
    private final XSSFCellStyle decimal;
    private final EstilosLinha normal;
    private final EstilosLinha alternada;

    private RelatorioClinicoWorkbook(XSSFWorkbook workbook) {
        this.workbook = workbook;
        titulo = estilo(VERDE, 16, true, HorizontalAlignment.LEFT);
        divisoria = workbook.createCellStyle();
        divisoria.setBorderBottom(BorderStyle.THIN);
        divisoria.setBottomBorderColor(LINHA);
        secao = estilo(VERDE, 11, true, HorizontalAlignment.LEFT);
        cabecalho = estilo(BRANCO, 10, true, HorizontalAlignment.CENTER);
        preencher(cabecalho, VERDE);
        cabecalho.setWrapText(true);
        contexto = estilo(TEXTO, 10, true, HorizontalAlignment.LEFT);
        nota = estilo(NOTA, 10, false, HorizontalAlignment.LEFT);
        data = estilo(TEXTO, 10, false, HorizontalAlignment.LEFT);
        data.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy"));
        decimal = estilo(TEXTO, 10, false, HorizontalAlignment.RIGHT);
        decimal.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        preencher(decimal, SUAVE);
        normal = estilosLinha(null);
        alternada = estilosLinha(SUAVE);
    }

    public static byte[] exportar(RelatorioClinicoResponse relatorio, ZoneId fuso) throws IOException {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var arquivo = new RelatorioClinicoWorkbook(workbook);
            arquivo.resumo(relatorio, fuso);
            arquivo.veterinarios(relatorio);
            arquivo.equipe(relatorio);
            arquivo.baias(relatorio);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void resumo(RelatorioClinicoResponse relatorio, ZoneId fuso) {
        var aba = aba("Resumo", "QRVet | Relatório da clínica", 37, 17, 4, 32, 16);
        aba.setTabColor(VERDE);
        var periodo = relatorio.periodo();
        celula(linha(aba, 4), 0, "Data inicial", contexto);
        data(linha(aba, 4), periodo.startDate(), fuso, "Sem data inicial");
        celula(linha(aba, 5), 0, "Data final", contexto);
        data(linha(aba, 5), periodo.endDate(), fuso, "Sem data final");
        celula(linha(aba, 4), 3, "Veterinário responsável", contexto);
        celula(linha(aba, 5), 3, veterinarioSelecionado(relatorio), normal.texto());

        var internacoes = relatorio.resumoInternacoes();
        celula(linha(aba, 7), 0, "Internações", secao);
        cabecalho(aba, 8, 0, "Indicador", "Total");
        indicador(aba, 9, 0, "Abertas no período", internacoes.totalNoPeriodo());
        indicador(aba, 10, 0, "Ainda ativas", internacoes.ativas());
        indicador(aba, 11, 0, "Com alta", internacoes.altas());
        indicador(aba, 12, 0, "Com óbito", internacoes.obitos());
        indicador(aba, 13, 0, "Encerradas no período", internacoes.encerradasNoPeriodo());
        celula(linha(aba, 14), 0, "Tempo médio de internação (h)", alternada.texto());
        celula(linha(aba, 14), 1, internacoes.tempoMedioInternacaoHoras(), decimal);

        var cuidados = relatorio.resumoCuidados();
        celula(linha(aba, 7), 3, "Cuidados clínicos", secao);
        cabecalho(aba, 8, 3, "Indicador", "Total");
        indicador(aba, 9, 3, "Evoluções registradas", cuidados.evolucoesRegistradas());
        indicador(aba, 10, 3, "Medicações aplicadas", cuidados.medicacoesAplicadas());
        indicador(aba, 11, 3, "Alimentações registradas", cuidados.alimentacoesRegistradas());
        indicador(aba, 12, 3, "Jejuns iniciados", cuidados.jejunsIniciados());
        indicador(aba, 13, 3, "Refeições agendadas", cuidados.refeicoesAgendadas());
        indicador(aba, 14, 3, "Refeições concluídas", cuidados.refeicoesConcluidas());
        indicador(aba, 15, 3, "Refeições pendentes", cuidados.refeicoesPendentes());
        indicador(aba, 16, 3, "Refeições canceladas", cuidados.refeicoesCanceladas());

        celula(linha(aba, 18), 0, "Os status e o tempo médio consideram as internações abertas no período.", nota);
        celula(linha(aba, 19), 0, "Os encerramentos são contados pela data de saída.", nota);
    }

    private void veterinarios(RelatorioClinicoResponse relatorio) {
        var aba = aba("Veterinários", "Atendimentos por veterinário", 30, 34, 16, 12, 12, 12, 15, 14);
        cabecalho(aba, LINHA_CABECALHO, 0, "Veterinário", "E-mail", "Internações", "Ativas", "Altas", "Óbitos", "Evoluções", "ID");
        int indice = LINHA_CABECALHO + 1;
        for (var vet : relatorio.atendimentosPorVeterinario()) {
            linhaDetalhe(aba, indice++, vet.veterinarioNome(), vet.veterinarioEmail(), vet.totalInternacoes(),
                    vet.ativas(), vet.altas(), vet.obitos(), vet.evolucoesRegistradas(), id(vet.veterinarioId()));
        }
        concluirDetalhes(aba, indice, 8, "Nenhum veterinário encontrado para estes filtros.");
    }

    private void equipe(RelatorioClinicoResponse relatorio) {
        var aba = aba("Equipe", "Produtividade da equipe", 30, 34, 20, 15, 15, 16, 16, 14);
        cabecalho(aba, LINHA_CABECALHO, 0, "Profissional", "E-mail", "Perfil", "Evoluções", "Medicações",
                "Alimentações", "Total de ações", "ID");
        int indice = LINHA_CABECALHO + 1;
        for (var membro : relatorio.produtividadeEquipe()) {
            linhaDetalhe(aba, indice++, membro.nome(), membro.email(), perfil(membro.role()),
                    membro.evolucoesRegistradas(), membro.medicacoesAplicadas(), membro.alimentacoesRegistradas(),
                    membro.totalAcoesClinicas(), id(membro.usuarioId()));
        }
        concluirDetalhes(aba, indice, 8, "Nenhuma ação clínica registrada para estes filtros.");
    }

    private void baias(RelatorioClinicoResponse relatorio) {
        var aba = aba("Baias", "Utilização das baias", 25, 21, 27, 14);
        cabecalho(aba, LINHA_CABECALHO, 0, "Baia", "Status atual", "Internações no período", "ID");
        int indice = LINHA_CABECALHO + 1;
        for (var baia : relatorio.utilizacaoBaias()) {
            linhaDetalhe(aba, indice++, baia.identificacao(), status(baia.statusAtual()),
                    baia.totalInternacoesNoPeriodo(), id(baia.baiaId()));
        }
        concluirDetalhes(aba, indice, 4, "Nenhuma baia cadastrada.");
    }

    private XSSFSheet aba(String nome, String tituloAba, int... larguras) {
        var aba = workbook.createSheet(nome);
        aba.setDisplayGridlines(false);
        aba.setDefaultRowHeightInPoints(19);
        linha(aba, 0).setHeightInPoints(8);
        var linhaTitulo = linha(aba, 1);
        linhaTitulo.setHeightInPoints(28);
        celula(linhaTitulo, 0, tituloAba, titulo);
        var linhaDivisoria = linha(aba, 2);
        linhaDivisoria.setHeightInPoints(5);
        for (int coluna = 0; coluna < larguras.length; coluna++) {
            aba.setColumnWidth(coluna, larguras[coluna] * 256);
            celula(linhaDivisoria, coluna, null, divisoria);
        }
        return aba;
    }

    private void cabecalho(XSSFSheet aba, int indice, int primeiraColuna, String... titulos) {
        var linha = linha(aba, indice);
        linha.setHeightInPoints(23);
        for (int coluna = 0; coluna < titulos.length; coluna++) {
            celula(linha, primeiraColuna + coluna, titulos[coluna], cabecalho);
        }
    }

    private void indicador(XSSFSheet aba, int indice, int coluna, String nome, long valor) {
        var estilos = estilosPara(indice);
        var linha = linha(aba, indice);
        celula(linha, coluna, nome, estilos.texto());
        celula(linha, coluna + 1, valor, estilos.numero());
    }

    private void linhaDetalhe(XSSFSheet aba, int indice, Object... valores) {
        var estilos = estilosPara(indice);
        var linha = linha(aba, indice);
        for (int coluna = 0; coluna < valores.length; coluna++) {
            Object valor = valores[coluna];
            celula(linha, coluna, valor, valor instanceof Number ? estilos.numero() : estilos.texto());
        }
    }

    private void concluirDetalhes(XSSFSheet aba, int proximaLinha, int colunas, String vazio) {
        if (proximaLinha == LINHA_CABECALHO + 1) {
            celula(linha(aba, proximaLinha), 0, vazio, nota);
            return;
        }
        aba.createFreezePane(0, LINHA_CABECALHO + 1);
        aba.setAutoFilter(new CellRangeAddress(LINHA_CABECALHO, proximaLinha - 1, 0, colunas - 1));
    }

    private void data(Row linha, Instant valor, ZoneId fuso, String semData) {
        if (valor == null) {
            celula(linha, 1, semData, nota);
            return;
        }
        var dia = valor.atZone(fuso).toLocalDate();
        if (dia.getYear() < 1900 || dia.getYear() > 9999) {
            celula(linha, 1, DATA_BR.format(dia), normal.texto());
        } else {
            celula(linha, 1, dia, data);
        }
    }

    private static String veterinarioSelecionado(RelatorioClinicoResponse relatorio) {
        Long id = relatorio.periodo().veterinarioId();
        if (id == null) return "Todos os veterinários";
        return relatorio.atendimentosPorVeterinario().stream()
                .filter(vet -> id.equals(vet.veterinarioId()))
                .map(RelatorioClinicoResponse.VeterinarioRelatorioResponse::veterinarioNome)
                .filter(Objects::nonNull)
                .findFirst().orElse("ID " + id);
    }

    private XSSFCellStyle estilo(XSSFColor cor, int tamanho, boolean negrito, HorizontalAlignment alinhamento) {
        var fonte = workbook.createFont();
        fonte.setFontName("Arial");
        fonte.setFontHeightInPoints((short) tamanho);
        fonte.setColor(cor);
        fonte.setBold(negrito);
        var estilo = workbook.createCellStyle();
        estilo.setFont(fonte);
        estilo.setAlignment(alinhamento);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        return estilo;
    }

    private EstilosLinha estilosLinha(XSSFColor fundo) {
        var texto = estilo(TEXTO, 10, false, HorizontalAlignment.LEFT);
        var numero = estilo(TEXTO, 10, false, HorizontalAlignment.RIGHT);
        numero.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        if (fundo != null) {
            preencher(texto, fundo);
            preencher(numero, fundo);
        }
        return new EstilosLinha(texto, numero);
    }

    private EstilosLinha estilosPara(int linha) {
        return linha % 2 == 0 ? alternada : normal;
    }

    private static void preencher(XSSFCellStyle estilo, XSSFColor cor) {
        estilo.setFillForegroundColor(cor);
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }

    private static XSSFColor cor(String rgb) {
        return new XSSFColor(HexFormat.of().parseHex(rgb), null);
    }

    private static Row linha(XSSFSheet aba, int indice) {
        var linha = aba.getRow(indice);
        return linha != null ? linha : aba.createRow(indice);
    }

    private static void celula(Row linha, int coluna, Object valor, XSSFCellStyle estilo) {
        var celula = linha.createCell(coluna);
        celula.setCellStyle(estilo);
        switch (valor) {
            case Number numero -> celula.setCellValue(numero.doubleValue());
            case LocalDate dia -> celula.setCellValue(dia);
            case String texto -> celula.setCellValue(texto);
            case null -> celula.setBlank();
            default -> throw new IllegalArgumentException("Tipo de célula inválido: " + valor.getClass());
        }
    }

    private static String id(Long valor) {
        return Objects.toString(valor, "");
    }

    private static String perfil(Role role) {
        return switch (role) {
            case ADMIN -> "Administrador";
            case VETERINARIO -> "Veterinário";
            case RECEPCIONISTA -> "Recepcionista";
            case AUXILIAR_TECNICO -> "Auxiliar técnico";
            case TUTOR -> "Tutor";
            case null -> "";
        };
    }

    private static String status(BaiaStatus status) {
        return switch (status) {
            case DISPONIVEL -> "Disponível";
            case OCUPADA -> "Ocupada";
            case MANUTENCAO -> "Manutenção";
            case null -> "";
        };
    }

    private record EstilosLinha(XSSFCellStyle texto, XSSFCellStyle numero) {}
}
