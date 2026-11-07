package ipiranga.fatec.qrvet.dtos.response;

import ipiranga.fatec.qrvet.models.RegistroMedicacao;
import java.time.Instant;

public record RegistroMedicacaoResponse(
        Long id,
        Long internacaoId,
        Long usuarioId,
        String responsavelNome,
        String medicamento,
        String dose,
        String unidade,
        String via,
        String prescricao,
        String observacoes,
        Instant dataHoraRegistro) {
    public static RegistroMedicacaoResponse from(RegistroMedicacao registro) {
        return new RegistroMedicacaoResponse(
                registro.getId(),
                registro.getInternacao().getId(),
                registro.getUsuario().getId(),
                registro.getUsuario().getName(),
                registro.getMedicamento(),
                registro.getDose(),
                registro.getUnidade(),
                registro.getVia(),
                registro.getPrescricao(),
                registro.getObservacoes(),
                registro.getDataHoraRegistro());
    }
}
