package ipiranga.fatec.qrvet.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TutorRequest(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Size(max = 14) String cpf,
        @NotBlank @Size(max = 20) String telefone,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 255) String endereco) {}
