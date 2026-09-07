package ipiranga.fatec.qrvet.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.web.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
                    "/v1/bootstrap/admin");

    private final CustomJwtAuthenticationConverter jwtConverter;

    @Value("${qrvet.frontend-url}")
    private String origin;

    @Value("${qrvet.cookie-secure}")
    private boolean secure;

    public SecurityConfig(CustomJwtAuthenticationConverter jwtConverter) {
        this.jwtConverter = jwtConverter;
    }

    @Bean
    SecretKey key(@Value("${qrvet.jwt-secret}") String secret) {
        byte[] bytes = Base64.getDecoder().decode(secret);
        if (bytes.length < 32)
            throw new IllegalStateException(
                    "JWT_SECRET must have at least 32 bytes in Base64.");
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
    SecurityFilterChain filtro(HttpSecurity http) {
        configureCors(http);
        configureSessions(http);
        configureCsrf(http);
        http.authorizeHttpRequests(a ->
                a.requestMatchers(PUBLIC_ENDPOINTS.toArray(new String[0]))
                .permitAll()
                .anyRequest()
                .authenticated())
            .oauth2ResourceServer(o ->
                    o.bearerTokenResolver(cookieBearerTokenResolver())
                            .jwt(j -> j.jwtAuthenticationConverter(jwtConverter)));

        return http.build();
    }

    private void configureCors(HttpSecurity http) {
        http.cors(c -> c.configurationSource(corsConfigurationSource()));
    }

    private void configureSessions(HttpSecurity http) {
        http.sessionManagement(
                session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }

    private void configureCsrf(HttpSecurity http) {
        http.csrf(
                csrf ->
                        csrf.csrfTokenRepository(csrfRepository())
                                .csrfTokenRequestHandler(
                                        new CsrfTokenRequestAttributeHandler()));
    }

    private CookieCsrfTokenRepository csrfRepository() {
        var csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookieCustomizer(c -> c.secure(secure).sameSite("Lax").path("/"));
        return csrf;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(origin));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN"));
        cors.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    private BearerTokenResolver cookieBearerTokenResolver() {
        return new DefaultBearerTokenResolver();
    }

}
