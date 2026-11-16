package ipiranga.fatec.qrvet.dtos.response;

import ipiranga.fatec.qrvet.models.Evolucao;
import java.time.Instant;

public record EvolucaoPublicResponse(String profissionalNome, String texto, Instant dataHoraRegistro) {
    public static EvolucaoPublicResponse from(Evolucao evolucao) {
        return new EvolucaoPublicResponse(evolucao.getProfissionalNome(), evolucao.getTexto(), evolucao.getDataHoraRegistro());
    }
}
