package ipiranga.fatec.qrvet.api;

import ipiranga.fatec.qrvet.model.Role;
import ipiranga.fatec.qrvet.model.UserAccount;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email, String phone, String avatarUrl,
                           Role role, String crmv, ClinicResponse clinic) {
    public static UserResponse from(UserAccount user) {
        String avatarUrl = user.getAvatarData() == null ? null : "/api/users/" + user.getId() + "/avatar";
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(), avatarUrl,
            user.getRole(), user.getCrmv(), new ClinicResponse(user.getClinic().getId(), user.getClinic().getName()));
    }

    public record ClinicResponse(UUID id, String name) {}
}
