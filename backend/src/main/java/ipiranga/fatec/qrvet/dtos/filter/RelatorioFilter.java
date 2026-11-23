package ipiranga.fatec.qrvet.dtos.filter;

import java.time.Instant;

public record RelatorioFilter(
        Instant startDate,
        Instant endDate,
        Long veterinarioId) {
}
