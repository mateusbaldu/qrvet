package ipiranga.fatec.qrvet.specifications;

import ipiranga.fatec.qrvet.dtos.filter.TutorSearchFilter;
import ipiranga.fatec.qrvet.models.Tutor;
import org.springframework.data.jpa.domain.Specification;
import java.util.Locale;

public final class TutorSpecifications {
    private TutorSpecifications() {}

    public static Specification<Tutor> matches(TutorSearchFilter filter) {
        if (filter == null) return Specification.unrestricted();
        return Specification.<Tutor>unrestricted()
                .and(contains("nome", filter.nome()))
                .and(cpf(filter.cpf()));
    }

    private static Specification<Tutor> contains(String field, String value) {
        if (value == null || value.isBlank()) return Specification.unrestricted();
        String pattern = "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, builder) -> builder.like(builder.lower(root.get(field)), pattern);
    }

    private static Specification<Tutor> cpf(String value) {
        if (value == null || value.isBlank()) return Specification.unrestricted();
        String normalized = value.replaceAll("\\D", "");
        return (root, query, builder) -> builder.like(root.get("cpf"), "%" + normalized + "%");
    }
}
