package ipiranga.fatec.qrvet.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("qrvetSecurity")
public class QRVetSecurity {
    public boolean hasAnyRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return false;

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
