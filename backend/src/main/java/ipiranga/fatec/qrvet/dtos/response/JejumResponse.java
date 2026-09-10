package ipiranga.fatec.qrvet.dtos.response;

import ipiranga.fatec.qrvet.models.Jejum;
import java.time.Instant;

public record JejumResponse(
        Long id,
        String motivo,
        Instant dataHoraInicio,
        Instant dataHoraFim,
        boolean ativo) {
    public static JejumResponse from(Jejum jejum) {
        return new JejumResponse(
                jejum.getId(),
                jejum.getMotivo(),
                jejum.getDataHoraInicio(),
                jejum.getDataHoraFim(),
                jejum.isAtivo());
    }
}
