package ipiranga.fatec.qrvet.dtos.request;

import jakarta.validation.constraints.*;
import java.time.Instant;

public record AgendaAlimentacaoUpdateRequest(
        @NotBlank @Size(max = 100) String alimento,
        @NotBlank @Size(max = 50) String quantidade,
        @NotNull Instant horario) {}
