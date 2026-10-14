package ipiranga.fatec.qrvet.specifications;

import ipiranga.fatec.qrvet.dtos.filter.AuditLogFilter;
import ipiranga.fatec.qrvet.models.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Locale;

public final class AuditLogSpecifications {
    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> matches(AuditLogFilter filter) {
        if (filter == null) {
            return Specification.unrestricted();
        }

        return Specification.<AuditLog>unrestricted()
                .and(afterOrEqual(filter.startDate()))
                .and(beforeOrEqual(filter.endDate()))
                .and(byUserId(filter.userId()))
                .and(byText("action", filter.action()))
                .and(byText("resourceName", filter.resourceName()));
    }

    private static Specification<AuditLog> afterOrEqual(Instant value) {
        if (value == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("timestamp"), value);
    }

    private static Specification<AuditLog> beforeOrEqual(Instant value) {
        if (value == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get("timestamp"), value);
    }

    private static Specification<AuditLog> byUserId(Long userId) {
        if (userId == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("userId"), userId);
    }

    private static Specification<AuditLog> byText(String attribute, String value) {
        if (value == null || value.isBlank()) {
            return Specification.unrestricted();
        }
        String pattern = "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, builder) ->
                builder.like(builder.lower(root.get(attribute)), pattern);
    }
}
