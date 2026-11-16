package ipiranga.fatec.qrvet.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {
    private final ipiranga.fatec.qrvet.repositories.UserRepository users;

    public CustomJwtAuthenticationConverter(ipiranga.fatec.qrvet.repositories.UserRepository users) {
        this.users = users;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        if (!"access".equals(jwt.getClaimAsString("typ")))
            throw new BadCredentialsException("Invalid access token");

        String role = jwt.getClaimAsString("role");
        if (role == null || role.isBlank())
            throw new BadCredentialsException("Invalid access token");

        try {
            role = users.findById(Long.valueOf(jwt.getSubject()))
                    .filter(user -> user.isActive() && user.isConfirmed())
                    .orElseThrow(() -> new BadCredentialsException("Inactive user"))
                    .getRole().name();
        } catch (NumberFormatException exception) {
            throw new BadCredentialsException("Invalid access token", exception);
        }

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}
