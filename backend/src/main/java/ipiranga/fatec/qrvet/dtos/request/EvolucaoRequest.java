package ipiranga.fatec.qrvet.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EvolucaoRequest(@NotBlank @Size(max = 10000) String texto) {}
