package ipiranga.fatec.qrvet.dtos.response;

import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.models.enums.Role;

public record UserResponse(
        Long id, String name, String email, Role role, boolean active, boolean confirmed) {
    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getRole(),
                u.isActive(),
                u.isConfirmed());
    }
}
