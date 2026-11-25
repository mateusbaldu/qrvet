package ipiranga.fatec.qrvet.dtos.request;

import ipiranga.fatec.qrvet.models.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UserRoleRequest(@NotNull Role role) {}
