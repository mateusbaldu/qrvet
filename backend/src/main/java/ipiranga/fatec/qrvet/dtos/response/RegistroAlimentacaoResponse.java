package ipiranga.fatec.qrvet.dtos.response;

import ipiranga.fatec.qrvet.models.RegistroAlimentacao;
import java.time.Instant;

public record RegistroAlimentacaoResponse(
        Long id,
        Long internacaoId,
        Long usuarioId,
        String alimento,
        String quantidade,
        String aceitacaoObservacao,
        Instant dataHoraRegistro) {
    public static RegistroAlimentacaoResponse from(RegistroAlimentacao registro) {
        return new RegistroAlimentacaoResponse(
                registro.getId(),
                registro.getInternacao().getId(),
                registro.getUsuario().getId(),
                registro.getAlimento(),
                registro.getQuantidade(),
                registro.getAceitacaoObservacao(),
                registro.getDataHoraRegistro());
    }
}
