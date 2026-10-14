package ipiranga.fatec.qrvet.dtos.request;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public record AgendaAlimentacaoRequest(
        @NotBlank @Size(max = 100) String alimento,
        @NotBlank @Size(max = 50) String quantidade,
        @NotEmpty @Size(max = 248) List<@NotNull @Future Instant> horarios) {}
