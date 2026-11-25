package ipiranga.fatec.qrvet.dtos.request;

import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(
        @NotNull(message = "User active status is required.")
        Boolean active
) {
}
