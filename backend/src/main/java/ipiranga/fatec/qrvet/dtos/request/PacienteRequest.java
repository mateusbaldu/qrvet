package ipiranga.fatec.qrvet.dtos.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PacienteRequest(
        @NotNull Long tutorId,
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Size(max = 50) String especie,
        @NotBlank @Size(max = 50) String raca,
        @NotBlank @Size(max = 20) String sexo,
        @NotNull @PastOrPresent LocalDate dataNascimento,
        @NotNull @DecimalMin("0.01") @Digits(integer = 4, fraction = 2) BigDecimal peso,
        @Size(max = 10000) String observacoes) {}
