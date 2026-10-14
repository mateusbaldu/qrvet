package ipiranga.fatec.qrvet.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import ipiranga.fatec.qrvet.models.AuditLog;
import ipiranga.fatec.qrvet.repositories.AuditLogRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditEventListenerTests {
    private final AuditLogRepository repository = mock(AuditLogRepository.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final AuditDetailsSanitizer sanitizer = mock(AuditDetailsSanitizer.class);
    private final AuditEventListener listener = new AuditEventListener(repository, objectMapper, sanitizer);

    @Test
    void persistsEventFieldsAndSerializedDetails() throws Exception {
        Map<String, Object> details = Map.of("name", "Luna");
        when(sanitizer.sanitize(details)).thenReturn(new TextNode("{\"name\":\"Luna\"}"));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"name\":\"Luna\"}");

        listener.handle(new AuditEvent(7L, "admin@qrvet.test", "127.0.0.1",
                "PATIENT_CREATED", "patient", "42", details));

        ArgumentCaptor<AuditLog> saved = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(saved.capture());
        assertEquals(7L, saved.getValue().getUserId());
        assertEquals("admin@qrvet.test", saved.getValue().getUserEmail());
        assertEquals("PATIENT_CREATED", saved.getValue().getAction());
        assertEquals("{\"name\":\"Luna\"}", saved.getValue().getDetails());
    }

    @Test
    void storesSafeFallbackWhenSerializationFails() throws Exception {
        when(sanitizer.sanitize(any())).thenReturn(new TextNode("safe"));
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("failure") {});

        listener.handle(new AuditEvent(null, null, null,
                "LOGIN_FAILED", "auth", null, Map.of("password", "secret")));

        ArgumentCaptor<AuditLog> saved = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(saved.capture());
        assertEquals("{\"error\":\"details_unavailable\"}", saved.getValue().getDetails());
    }

    @Test
    void leavesDetailsNullWhenEventHasNoDetails() {
        listener.handle(new AuditEvent(null, null, null,
                "LOGOUT", "auth", "7", null));

        ArgumentCaptor<AuditLog> saved = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(saved.capture());
        assertEquals(null, saved.getValue().getDetails());
    }
}
