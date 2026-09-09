package ipiranga.fatec.qrvet.dtos.response;

import ipiranga.fatec.qrvet.models.Baia;
import ipiranga.fatec.qrvet.models.enums.BaiaStatus;

public record BaiaResponse(Long id, String identificacao, BaiaStatus status, String observacao) {
    public static BaiaResponse from(Baia baia) {
        return new BaiaResponse(baia.getId(), baia.getIdentificacao(), baia.getStatus(), baia.getObservacao());
    }
}
