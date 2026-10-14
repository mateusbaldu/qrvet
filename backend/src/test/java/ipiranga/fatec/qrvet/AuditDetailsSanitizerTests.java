package ipiranga.fatec.qrvet;

import com.fasterxml.jackson.databind.ObjectMapper;
import ipiranga.fatec.qrvet.event.AuditDetailsSanitizer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditDetailsSanitizerTests {
    private final AuditDetailsSanitizer sanitizer = new AuditDetailsSanitizer(new ObjectMapper());

    @Test
    void redactsSensitiveFieldsRecursively() {
        var details = Map.of(
                "email", "user@example.com",
                "password", "plain-secret",
                "nested", Map.of("recoveryToken", "token-value"),
                "items", List.of(Map.of("clientSecret", "secret-value")));

        var sanitized = sanitizer.sanitize(details);

        assertEquals("user@example.com", sanitized.get("email").asText());
        assertEquals("[REDACTED]", sanitized.get("password").asText());
        assertEquals("[REDACTED]", sanitized.get("nested").get("recoveryToken").asText());
        assertEquals("[REDACTED]", sanitized.get("items").get(0).get("clientSecret").asText());
    }
}
