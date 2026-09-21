package ipiranga.fatec.qrvet.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JejumRequest(@NotBlank @Size(max = 255) String motivo) {
}
