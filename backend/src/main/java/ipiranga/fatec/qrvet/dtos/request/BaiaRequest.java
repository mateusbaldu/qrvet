package ipiranga.fatec.qrvet.dtos.request;

import ipiranga.fatec.qrvet.models.enums.BaiaStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BaiaRequest(
        @NotBlank @Size(max = 50) String identificacao,
        @Size(max = 255) String observacao,
        BaiaStatus status) {
}
