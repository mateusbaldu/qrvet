package ipiranga.fatec.qrvet.dtos.filter;

import ipiranga.fatec.qrvet.models.enums.Role;

public record UserSearchFilter(
        String name,
        String email,
        Role role,
        Boolean active) {
}
