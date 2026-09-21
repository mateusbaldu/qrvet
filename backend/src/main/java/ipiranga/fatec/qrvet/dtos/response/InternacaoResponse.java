package ipiranga.fatec.qrvet.dtos.response;

import ipiranga.fatec.qrvet.models.Internacao;
import ipiranga.fatec.qrvet.models.enums.InternacaoStatus;
import java.time.Instant;

public record InternacaoResponse(
        Long id,
        Long pacienteId,
        Long baiaId,
        Long veterinarioId,
        String uuidToken,
        Instant entradaInternacao,
        Instant saidaInternacao,
        String motivo,
        String diagnosticoInicial,
        InternacaoStatus status,
        String observacoes) {
    public static InternacaoResponse from(Internacao internacao) {
        return new InternacaoResponse(
                internacao.getId(),
                internacao.getPaciente().getId(),
                internacao.getBaia().getId(),
                internacao.getVeterinario().getId(),
                internacao.getUuidToken().toString(),
                internacao.getEntradaInternacao(),
                internacao.getSaidaInternacao(),
                internacao.getMotivo(),
                internacao.getDiagnosticoInicial(),
                internacao.getStatus(),
                internacao.getObservacoes());
    }
}
