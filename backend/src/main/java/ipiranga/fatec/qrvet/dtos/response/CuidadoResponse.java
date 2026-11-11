package ipiranga.fatec.qrvet.dtos.response;

import ipiranga.fatec.qrvet.models.Internacao;
import ipiranga.fatec.qrvet.models.enums.InternacaoStatus;

// Identificação necessária ao cuidado, sem dados do tutor ou diagnóstico.
public record CuidadoResponse(Long id, String pacienteNome, String baiaIdentificacao, InternacaoStatus status, AlertasPacienteResponse alertas, String fotoVersao) {
    public static CuidadoResponse from(Internacao internacao) {
        return new CuidadoResponse(internacao.getId(), internacao.getPaciente().getNome(),
                internacao.getBaia().getIdentificacao(), internacao.getStatus(), AlertasPacienteResponse.from(internacao.getPaciente()),
                internacao.getPaciente().getFotoArquivo());
    }
}
