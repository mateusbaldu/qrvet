package ipiranga.fatec.qrvet.dtos.response;

import ipiranga.fatec.qrvet.models.Evolucao;
import java.time.Instant;

public record EvolucaoResponse(Long id, Long internacaoId, Long profissionalId, String profissionalNome,
                              String texto, Instant dataHoraRegistro) {
    public static EvolucaoResponse from(Evolucao evolucao) {
        return new EvolucaoResponse(evolucao.getId(), evolucao.getInternacao().getId(), evolucao.getUsuario().getId(),
                evolucao.getProfissionalNome(), evolucao.getTexto(), evolucao.getDataHoraRegistro());
    }
}
