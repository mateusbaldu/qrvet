package ipiranga.fatec.qrvet.controllers;

import ipiranga.fatec.qrvet.dtos.request.*;
import ipiranga.fatec.qrvet.dtos.response.*;
import ipiranga.fatec.qrvet.dtos.*;
import ipiranga.fatec.qrvet.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {
    private final AuthService auth;
    private final boolean secure;
    private static final long REFRESH_DAYS = 5;

    public AuthController(AuthService auth, @Value("${qrvet.cookie-secure}") boolean secure) {
        this.auth = auth;
        this.secure = secure;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    private String cookie(String token, Duration maxAge) {
        return ResponseCookie.from("QRVET_REFRESH", token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build()
                .toString();
    }

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(@Valid @RequestBody LoginRequest r) {
        AuthSession session = auth.login(r);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(session.refreshToken(), Duration.ofDays(REFRESH_DAYS)))
                .body(session.tokens());
    }

    @PostMapping("/refresh")
    public AccessTokenResponse refresh(
            @CookieValue(value = "QRVET_REFRESH", required = false) String refreshToken) {
        return auth.refreshAccessToken(refreshToken);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = "QRVET_REFRESH", required = false) String refreshToken) {
        auth.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO))
                .build();
    }

    @GetMapping("/me")
    public UserResponse currentUser() {
        return auth.currentUser();
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat() {
        auth.heartbeat();
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/password")
    public ResponseEntity<Void> password(
            @Valid @RequestBody PasswordRequest r,
            @CookieValue(value = "QRVET_REFRESH", required = false) String refreshToken) {
        auth.changePassword(r);
        return logout(refreshToken);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> requestRecovery(@Valid @RequestBody ForgotPasswordRequest request) {
        auth.requestRecovery(request);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new MessageResponse("An email has been sent to your account."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        auth.resetPassword(request);

        return ResponseEntity.noContent().build();
    }
}
