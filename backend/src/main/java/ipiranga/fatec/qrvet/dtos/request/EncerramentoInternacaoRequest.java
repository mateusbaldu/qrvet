package ipiranga.fatec.qrvet.dtos.request;

import ipiranga.fatec.qrvet.models.enums.StatusEncerramento;
import jakarta.validation.constraints.NotNull;

public record EncerramentoInternacaoRequest(@NotNull StatusEncerramento statusEncerramento) {
}
