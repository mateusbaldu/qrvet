package ipiranga.fatec.qrvet.specifications;

import ipiranga.fatec.qrvet.models.RegistroAlimentacao;
import org.springframework.data.jpa.domain.Specification;

public final class RegistroAlimentacaoSpecifications {
    private RegistroAlimentacaoSpecifications() {}

    public static Specification<RegistroAlimentacao> byInternacaoId(Long internacaoId) {
        return (root, query, builder) ->
                builder.equal(root.get("internacao").get("id"), internacaoId);
    }
}
