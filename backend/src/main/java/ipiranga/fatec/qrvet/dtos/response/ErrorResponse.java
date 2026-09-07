package ipiranga.fatec.qrvet.dtos.response;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        String codigo,
        String message,
        Map<String, String> fields,
        Instant horario,
        String requisicaoId) {}
