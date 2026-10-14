package ipiranga.fatec.qrvet.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

@Component
public class AuditDetailsSanitizer {
    private static final String REDACTED = "[REDACTED]";
    private final ObjectMapper objectMapper;

    public AuditDetailsSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode sanitize(Object details) {
        JsonNode node = objectMapper.valueToTree(details);
        sanitizeNode(node);
        return node;
    }

    private void sanitizeNode(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSensitive(field.getKey())) {
                    objectNode.put(field.getKey(), REDACTED);
                } else {
                    sanitizeNode(field.getValue());
                }
            }
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(this::sanitizeNode);
        }
    }

    private boolean isSensitive(String fieldName) {
        String normalized = fieldName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
        return normalized.contains("password")
                || normalized.contains("senha")
                || normalized.contains("token")
                || normalized.contains("secret")
                || normalized.contains("credential")
                || normalized.contains("credencial");
    }
}
