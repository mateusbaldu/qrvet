package ipiranga.fatec.qrvet.dtos.response;

import ipiranga.fatec.qrvet.models.Internacao;
import ipiranga.fatec.qrvet.models.enums.InternacaoStatus;
import java.time.Instant;

public record InternacaoPublicResponse(
        String pacienteNome,
        String especie,
        String raca,
        String sexo,
        String baiaIdentificacao,
        String motivo,
        InternacaoStatus status,
        boolean jejumAtivo,
        Instant entradaInternacao) {
    public static InternacaoPublicResponse from(Internacao internacao, boolean jejumAtivo) {
        return new InternacaoPublicResponse(
                internacao.getPaciente().getNome(),
                internacao.getPaciente().getEspecie(),
                internacao.getPaciente().getRaca(),
                internacao.getPaciente().getSexo(),
                internacao.getBaia().getIdentificacao(),
                internacao.getMotivo(),
                internacao.getStatus(),
                jejumAtivo,
                internacao.getEntradaInternacao());
    }
}
