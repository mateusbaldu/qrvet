package ipiranga.fatec.qrvet.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordRequest(
        @NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 72) String newPassword) {}
