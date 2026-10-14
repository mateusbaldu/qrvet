package ipiranga.fatec.qrvet.exceptions;

import ipiranga.fatec.qrvet.dtos.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTests {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handledExceptionReturnsBadRequestAndWritesWarning(CapturedOutput output) {
        ResponseEntity<ErrorResponse> response =
                handler.invalidRequest(new InvalidRequestException("Invalid request"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("BAD_REQUEST", response.getBody().codigo());
        assertTrue(output.getOut().contains("WARN"));
        assertTrue(output.getOut().contains("Invalid application request"));
    }

    @Test
    void unexpectedExceptionReturnsInternalServerErrorAndWritesError(CapturedOutput output) {
        ResponseEntity<ErrorResponse> response =
                handler.unexpected(new IllegalStateException("unexpected failure"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().codigo());
        assertTrue(output.getOut().contains("ERROR"));
        assertTrue(output.getOut().contains("java.lang.IllegalStateException"));
    }

    @Test
    void handledLogsDoNotIncludeSensitiveValues(CapturedOutput output) {
        String password = "plain-password-value";
        String token = "recovery-token-value";

        handler.invalidRequest(new InvalidRequestException("Invalid request"));
        handler.unexpected(new IllegalStateException("Operation failed"));

        assertFalse(output.getOut().contains(password));
        assertFalse(output.getOut().contains(token));
    }

    @Test
    void unexpectedLogsRedactSensitiveExceptionValues(CapturedOutput output) {
        String password = "plain-password-value";
        String token = "recovery-token-value";
        String secret = "application-secret-value";
        String credential = "credential-value";

        handler.unexpected(new IllegalStateException(
                "password=" + password + " token=" + token
                        + " secret=" + secret + " credential=" + credential));

        assertFalse(output.getOut().contains(password));
        assertFalse(output.getOut().contains(token));
        assertFalse(output.getOut().contains(secret));
        assertFalse(output.getOut().contains(credential));
    }
}
