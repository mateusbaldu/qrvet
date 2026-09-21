package ipiranga.fatec.qrvet.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import ipiranga.fatec.qrvet.security.CustomJwtAuthenticationConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private static final List<String> PUBLIC_ENDPOINTS =
            List.of(
                    "/v1/auth/login",
                    "/v1/auth/csrf",
                    "/v1/auth/logout",
                    "/v1/auth/refresh",
                    "/v1/auth/forgot-password",
                    "/v1/auth/reset-password",
                    "/v1/auth/confirm-invitation",
                    "/v1/bootstrap/admin",
                    "/v1/public/internacoes/qr/**");

    private final CustomJwtAuthenticationConverter jwtConverter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final boolean secure;

    public SecurityConfig(
            CustomJwtAuthenticationConverter jwtConverter,
            CorsConfigurationSource corsConfigurationSource,
            @Value("${qrvet.cookie-secure}") boolean secure) {
        this.jwtConverter = jwtConverter;
        this.corsConfigurationSource = corsConfigurationSource;
        this.secure = secure;
    }

    @Bean
    SecretKey key(@Value("${qrvet.jwt-secret}") String secret) {
        byte[] bytes = Base64.getDecoder().decode(secret);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must have at least 32 bytes in Base64.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder encoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    JwtDecoder decoder(SecretKey key) {
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("qrvet"));
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        configureCors(http);
        configureSessions(http);
        configureCsrf(http);
        http.authorizeHttpRequests(authorize ->
                        authorize.requestMatchers(PUBLIC_ENDPOINTS.toArray(new String[0]))
                                .permitAll()
                                .anyRequest()
                                .authenticated())
                .oauth2ResourceServer(oauth ->
                        oauth.bearerTokenResolver(bearerTokenResolver())
                                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)));

        return http.build();
    }

    private void configureCors(HttpSecurity http) {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource));
    }

    private void configureSessions(HttpSecurity http) {
        http.sessionManagement(
                session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }

    private void configureCsrf(HttpSecurity http) {
        http.csrf(csrf ->
                csrf.csrfTokenRepository(csrfRepository())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));
    }

    private CookieCsrfTokenRepository csrfRepository() {
        var csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookieCustomizer(cookie -> cookie.secure(secure).sameSite("Lax").path("/"));
        return csrf;
    }

    private BearerTokenResolver bearerTokenResolver() {
        return new DefaultBearerTokenResolver();
    }
}
