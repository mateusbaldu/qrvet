package ipiranga.fatec.qrvet.dtos.response;

import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.models.enums.Role;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        boolean active,
        boolean confirmed,
        Instant lastActivityAt,
        Instant createdAt,
        Instant updatedAt) {
    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getRole(),
                u.isActive(),
                u.isConfirmed(),
                u.getLastActivityAt(),
                u.getCreatedAt(),
                u.getUpdatedAt());
    }
}
