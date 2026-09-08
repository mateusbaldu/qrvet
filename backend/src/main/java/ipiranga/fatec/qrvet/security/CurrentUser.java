package ipiranga.fatec.qrvet.security;

import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.repositories.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    public User getCurrent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            throw new BadCredentialsException("User not authenticated");

        return users.findById(Long.valueOf(auth.getName()))
                .filter(User::isActive)
                .orElseThrow(() -> new BadCredentialsException("This user account is inactive"));
    }
}
