package ipiranga.fatec.qrvet.api;

import ipiranga.fatec.qrvet.security.AuthenticatedUser;
import ipiranga.fatec.qrvet.service.AuthService;
import ipiranga.fatec.qrvet.repository.UserAccountRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "qrvet_refresh";
    private final AuthService authService;
    private final UserAccountRepository users;
    private final boolean secureCookie;
    private final long refreshDays;

    public AuthController(AuthService authService, UserAccountRepository users,
                          @Value("${app.cookie.secure}") boolean secureCookie,
                          @Value("${app.jwt.refresh-days}") long refreshDays) {
        this.authService = authService;
        this.users = users;
        this.secureCookie = secureCookie;
        this.refreshDays = refreshDays;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return respond(authService.login(request.email(), request.password()), response);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refresh,
                                HttpServletResponse response) {
        if (refresh == null) throw new ipiranga.fatec.qrvet.common.ApiException(HttpStatus.UNAUTHORIZED,
            "MISSING_REFRESH_TOKEN", "A sessão expirou. Faça login novamente.");
        return respond(authService.refresh(refresh), response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refresh,
                                       HttpServletResponse response) {
        authService.logout(refresh);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return UserResponse.from(users.findById(principal.id()).orElseThrow());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.accepted().body(new MessageResponse(
            "Se o e-mail estiver cadastrado, enviaremos as instruções de recuperação."));
    }

    @PostMapping("/reset-password")
    public MessageResponse reset(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return new MessageResponse("Senha alterada com sucesso.");
    }

    @PostMapping("/accept-invitation")
    public ResponseEntity<MessageResponse> accept(@Valid @RequestBody AcceptInvitationRequest request) {
        authService.acceptInvitation(request.token(), request.name(), request.password(), request.phone(), request.crmv());
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("Conta criada com sucesso."));
    }

    private AuthResponse respond(AuthService.AuthResult result, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
            cookie(result.refreshToken(), Duration.ofDays(refreshDays)).toString());
        return new AuthResponse(result.accessToken(), "Bearer", result.expiresIn(), UserResponse.from(result.user()));
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
            .httpOnly(true).secure(secureCookie).sameSite("Lax").path("/api/auth")
            .maxAge(maxAge).build();
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
    public record ForgotPasswordRequest(@Email @NotBlank String email) {}
    public record ResetPasswordRequest(@NotBlank String token, @NotBlank String newPassword) {}
    public record AcceptInvitationRequest(@NotBlank String token, @NotBlank @Size(max = 160) String name,
                                          @NotBlank String password, String phone, String crmv) {}
    public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {}
    public record MessageResponse(String message) {}
}
