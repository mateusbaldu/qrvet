package ipiranga.fatec.qrvet.security;

import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.models.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.*;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TokenServiceTests {
    private final JwtEncoder encoder = mock(JwtEncoder.class);
    private final JwtDecoder decoder = mock(JwtDecoder.class);
    private final TokenService service = new TokenService(encoder, decoder);

    @Test
    void generatesAccessTokenWithExpectedClaims() {
        when(encoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt("access-token"));
        User user = User.builder().email("user@test").name("User").role(Role.VETERINARIO).build();
        setUserId(user, 7L);
        Instant issuedAt = Instant.parse("2026-01-01T10:00:00Z");

        assertEquals("access-token", service.generateAccessToken(user, issuedAt));

        var captor = org.mockito.ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(encoder).encode(captor.capture());
        JwtClaimsSet claims = captor.getValue().getClaims();
        assertEquals("qrvet", claims.getClaim("iss"));
        assertEquals("7", claims.getSubject());
        assertEquals("access", claims.getClaimAsString("typ"));
        assertEquals("VETERINARIO", claims.getClaimAsString("role"));
        assertEquals(issuedAt.plusSeconds(15 * 60), claims.getExpiresAt());
    }

    @Test
    void generatesRefreshTokenWithJtiAndRefreshType() {
        when(encoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt("refresh-token"));
        User user = User.builder().email("user@test").name("User").role(Role.ADMIN).build();
        setUserId(user, 9L);
        Instant issuedAt = Instant.parse("2026-01-01T10:00:00Z");

        var refresh = service.generateRefreshToken(user, issuedAt);

        assertEquals("refresh-token", refresh.value());
        assertNotNull(refresh.jti());
        var captor = org.mockito.ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(encoder).encode(captor.capture());
        JwtClaimsSet claims = captor.getValue().getClaims();
        assertEquals("refresh", claims.getClaimAsString("typ"));
        assertEquals("9", claims.getSubject());
        assertEquals(refresh.jti(), claims.getId());
        assertEquals(issuedAt.plusSeconds(5 * 24 * 60 * 60), claims.getExpiresAt());
    }

    @Test
    void rejectsBlankInvalidOrNonRefreshTokens() {
        assertThrows(BadCredentialsException.class, () -> service.decodeRefreshToken(null));
        assertThrows(BadCredentialsException.class, () -> service.decodeRefreshToken(" "));

        when(decoder.decode("bad")).thenThrow(new JwtException("invalid"));
        assertThrows(BadCredentialsException.class, () -> service.decodeRefreshToken("bad"));

        Jwt access = mock(Jwt.class);
        when(access.getClaimAsString("typ")).thenReturn("access");
        when(access.getId()).thenReturn("jti");
        when(decoder.decode("access")).thenReturn(access);
        assertThrows(BadCredentialsException.class, () -> service.decodeRefreshToken("access"));
    }

    @Test
    void acceptsRefreshTokenWithTypeAndId() {
        Jwt refresh = mock(Jwt.class);
        when(refresh.getClaimAsString("typ")).thenReturn("refresh");
        when(refresh.getId()).thenReturn("jti");
        when(decoder.decode("refresh")).thenReturn(refresh);

        assertSame(refresh, service.decodeRefreshToken("refresh"));
    }

    private static Jwt jwt(String token) {
        return Jwt.withTokenValue(token)
                .header("alg", "HS256")
                .claim("typ", "refresh")
                .subject("1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    private static void setUserId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
