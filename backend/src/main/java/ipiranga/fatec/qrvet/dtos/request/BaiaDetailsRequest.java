package ipiranga.fatec.qrvet.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BaiaDetailsRequest(
        @NotBlank @Size(max = 50) String identificacao,
        @Size(max = 255) String observacao) {
}
