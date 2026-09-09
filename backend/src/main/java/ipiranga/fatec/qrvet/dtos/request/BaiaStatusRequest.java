package ipiranga.fatec.qrvet.dtos.request;

import ipiranga.fatec.qrvet.models.enums.BaiaStatus;
import jakarta.validation.constraints.NotNull;

public record BaiaStatusRequest(@NotNull BaiaStatus status) {
}
