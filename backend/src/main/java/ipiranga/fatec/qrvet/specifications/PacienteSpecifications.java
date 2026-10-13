package ipiranga.fatec.qrvet.specifications;

import ipiranga.fatec.qrvet.dtos.filter.PacienteSearchFilter;
import ipiranga.fatec.qrvet.models.Internacao;
import ipiranga.fatec.qrvet.models.Paciente;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.util.Locale;

public final class PacienteSpecifications {
    private PacienteSpecifications() {}

    public static Specification<Paciente> matches(PacienteSearchFilter filter) {
        if (filter == null) return Specification.unrestricted();
        return Specification.<Paciente>unrestricted()
                .and(containsAny(filter.q()))
                .and(tutor(filter.tutorId()))
                .and(active(filter.active()));
    }

    private static Specification<Paciente> containsAny(String value) {
        if (value == null || value.isBlank()) return Specification.unrestricted();
        String pattern = "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("nome")), pattern),
                builder.like(builder.lower(root.get("especie")), pattern),
                builder.like(builder.lower(root.get("raca")), pattern),
                builder.like(builder.lower(root.join("tutor").get("nome")), pattern));
    }

    private static Specification<Paciente> tutor(Long tutorId) {
        if (tutorId == null) return Specification.unrestricted();
        return (root, query, builder) -> builder.equal(root.get("tutor").get("id"), tutorId);
    }

    private static Specification<Paciente> active(Boolean active) {
        if (active == null) return Specification.unrestricted();
        return (root, query, builder) -> {
            jakarta.persistence.criteria.Subquery<Long> subquery = query.subquery(Long.class);
            Root<Internacao> admission = subquery.from(Internacao.class);
            subquery.select(admission.get("id"));
            subquery.where(
                    builder.equal(admission.get("paciente").get("id"), root.get("id")),
                    builder.equal(admission.get("status"), ipiranga.fatec.qrvet.models.enums.InternacaoStatus.ATIVA));
            Predicate pacienteAtivo = builder.exists(subquery);
            if (active) {
                return pacienteAtivo;
            }
            return builder.not(pacienteAtivo);
        };
    }
}