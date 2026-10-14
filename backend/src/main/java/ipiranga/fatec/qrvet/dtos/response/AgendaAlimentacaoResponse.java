package ipiranga.fatec.qrvet.dtos.response;

import ipiranga.fatec.qrvet.models.AlimentacaoAgendada;
import ipiranga.fatec.qrvet.models.Internacao;
import ipiranga.fatec.qrvet.models.RegistroAlimentacao;
import java.time.Instant;

public record AgendaAlimentacaoResponse(Long id, Long internacaoId, String pacienteNome, String baiaIdentificacao,
        String internacaoStatus, String alimento, String quantidade, Instant horario, String status, boolean jejumAtivo,
        Instant concluidaEm, String responsavelNome, String observacao, String motivoCancelamento) {
    public static AgendaAlimentacaoResponse from(AlimentacaoAgendada a, boolean fasting) {
        Internacao i = a.getInternacao();
        RegistroAlimentacao r = a.getRegistro();
        Instant registroData = null;
        String usuarioNome = null;
        String observacao = null;

        if (r != null) {
            registroData = r.getDataHoraRegistro();
            usuarioNome = r.getUsuario().getName();
            observacao = r.getAceitacaoObservacao();
        }

        return new AgendaAlimentacaoResponse(a.getId(), i.getId(), i.getPaciente().getNome(), i.getBaia().getIdentificacao(),
                i.getStatus().name(), a.getAlimento(), a.getQuantidade(), a.getHorario(), a.getStatus().name(), fasting,
                registroData, usuarioNome, observacao, a.getMotivoCancelamento());
    }
}
