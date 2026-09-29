package ipiranga.fatec.qrvet.dtos.response;

import ipiranga.fatec.qrvet.models.AlimentacaoAgendada;
import java.time.Instant;

public record AgendaAlimentacaoResponse(Long id, Long internacaoId, String pacienteNome, String baiaIdentificacao,
        String internacaoStatus, String alimento, String quantidade, Instant horario, String status, boolean jejumAtivo,
        Instant concluidaEm, String responsavelNome, String observacao, String motivoCancelamento) {
    public static AgendaAlimentacaoResponse from(AlimentacaoAgendada a, boolean fasting) {
        var i = a.getInternacao();
        var r = a.getRegistro();
        return new AgendaAlimentacaoResponse(a.getId(), i.getId(), i.getPaciente().getNome(), i.getBaia().getIdentificacao(),
                i.getStatus().name(), a.getAlimento(), a.getQuantidade(), a.getHorario(), a.getStatus().name(), fasting,
                r == null ? null : r.getDataHoraRegistro(), r == null ? null : r.getUsuario().getName(),
                r == null ? null : r.getAceitacaoObservacao(), a.getMotivoCancelamento());
    }
}
