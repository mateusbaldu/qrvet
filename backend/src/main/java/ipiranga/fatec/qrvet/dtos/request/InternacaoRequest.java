package ipiranga.fatec.qrvet.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InternacaoRequest(
        @NotNull Long pacienteId,
        @NotNull Long baiaId,
        @NotNull Long veterinarioId,
        @NotBlank @Size(max = 10000) String motivo,
        @NotBlank @Size(max = 255) String diagnosticoInicial,
        @Size(max = 10000) String observacoes) {
}
