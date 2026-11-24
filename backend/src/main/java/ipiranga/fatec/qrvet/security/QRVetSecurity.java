package ipiranga.fatec.qrvet.security;

import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("qrvetSecurity")
public class QRVetSecurity {
    private final UserRepository users;

    public QRVetSecurity(UserRepository users) {
        this.users = users;
    }

    public boolean hasAnyRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return false;

        try {
            Long userId = Long.valueOf(authentication.getName());
            User user = users.findById(userId).orElse(null);
            if (user == null || !user.isActive() || !user.isConfirmed()) {
                return false;
            }
        } catch (NumberFormatException ignored) {
            return false;
        }

        for (String role : roles) {
            if (authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role)
                            || authority.getAuthority().equals(role))) {
                return true;
            }
        }
        return false;
    }
}
