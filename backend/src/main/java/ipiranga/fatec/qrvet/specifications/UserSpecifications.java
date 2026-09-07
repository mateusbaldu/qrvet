package ipiranga.fatec.qrvet.specifications;

import ipiranga.fatec.qrvet.dtos.filter.UserSearchFilter;
import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.models.enums.Role;
import jakarta.persistence.criteria.Expression;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {
    private static final char LIKE_ESCAPE = '\\';

    private UserSpecifications() {}

    public static Specification<User> matches(UserSearchFilter filter) {
        if (filter == null) {
            return Specification.unrestricted();
        }

        return Specification.<User>unrestricted()
                .and(filterByName(filter.name()))
                .and(filterByEmail(filter.email()))
                .and(filterByRole(filter.role()))
                .and(filterByActive(filter.active()));
    }

    private static Specification<User> filterByName(String name) {
        return containsIgnoreCase("name", name);
    }

    private static Specification<User> filterByEmail(String email) {
        return containsIgnoreCase("email", email);
    }

    private static Specification<User> filterByRole(Role role) {
        if (role == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("role"), role);
    }

    private static Specification<User> filterByActive(Boolean active) {
        if (active == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("active"), active);
    }

    private static Specification<User> containsIgnoreCase(String attribute, String value) {
        if (value == null || value.trim().isEmpty()) {
            return Specification.unrestricted();
        }

        String pattern = "%" + escapeLikePattern(value.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, builder) -> {
            Expression<String> field = builder.lower(root.<String>get(attribute));
            return builder.like(field, pattern, LIKE_ESCAPE);
        };
    }

    private static String escapeLikePattern(String value) {
        return value
                .replace(String.valueOf(LIKE_ESCAPE), String.valueOf(LIKE_ESCAPE) + LIKE_ESCAPE)
                .replace("%", String.valueOf(LIKE_ESCAPE) + "%")
                .replace("_", String.valueOf(LIKE_ESCAPE) + "_");
    }
}
