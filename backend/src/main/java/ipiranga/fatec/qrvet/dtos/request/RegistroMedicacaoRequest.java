package ipiranga.fatec.qrvet.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroMedicacaoRequest(
        @NotBlank @Size(max = 100) String medicamento,
        @NotBlank @Size(max = 50) String dose,
        @NotBlank @Size(max = 30) String unidade,
        @NotBlank @Size(max = 50) String via,
        @NotBlank @Size(max = 255) String prescricao,
        @Size(max = 10000) String observacoes) {
}
