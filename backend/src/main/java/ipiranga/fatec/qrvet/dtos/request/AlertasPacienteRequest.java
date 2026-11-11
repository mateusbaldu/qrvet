package ipiranga.fatec.qrvet.dtos.request;

import jakarta.validation.constraints.Size;

// PUT replaces all three fields; null or blank clears an alert.
public record AlertasPacienteRequest(
        @Size(max = 1000) String alergias,
        @Size(max = 1000) String restricoesAlimentares,
        @Size(max = 1000) String cuidadosManejo) {}
