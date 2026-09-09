package ipiranga.fatec.qrvet.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroAlimentacaoRequest(
        @NotBlank @Size(max = 100) String alimento,
        @NotBlank @Size(max = 50) String quantidade,
        @Size(max = 10000) String aceitacaoObservacao) {
}
