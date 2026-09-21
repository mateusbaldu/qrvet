package ipiranga.fatec.qrvet.security;

import ipiranga.fatec.qrvet.dtos.RefreshToken;
import ipiranga.fatec.qrvet.models.User;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class TokenService {
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(5);
    private static final String ISSUER = "qrvet";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    public TokenService(JwtEncoder encoder, JwtDecoder decoder) {
        this.encoder = encoder;
        this.decoder = decoder;
    }

    public String generateAccessToken(User user, Instant issuedAt) {
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(ISSUER)
                        .subject(user.getId().toString())
                        .issuedAt(issuedAt)
                        .expiresAt(issuedAt.plus(ACCESS_TOKEN_TTL))
                        .claim("typ", "access")
                        .claim("role", user.getRole().name())
                        .build();
        return encode(claims);
    }

    public RefreshToken generateRefreshToken(User user, Instant issuedAt) {
        String jti = UUID.randomUUID().toString();
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(ISSUER)
                        .subject(user.getId().toString())
                        .id(jti)
                        .issuedAt(issuedAt)
                        .expiresAt(issuedAt.plus(REFRESH_TOKEN_TTL))
                        .claim("typ", "refresh")
                        .build();
        return new RefreshToken(encode(claims), jti);
    }

    public Jwt decodeRefreshToken(String token) {
        if (token == null || token.isBlank())
            throw new BadCredentialsException("Invalid refresh token");
        try {
            Jwt jwt = decoder.decode(token);
            if (!"refresh".equals(jwt.getClaimAsString("typ")) || jwt.getId() == null)
                throw new BadCredentialsException("Invalid refresh token");
            return jwt;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid refresh token", e);
        }
    }

    private String encode(JwtClaimsSet claims) {
        return encoder.encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
