package ipiranga.fatec.qrvet.dtos.response;

import ipiranga.fatec.qrvet.models.Tutor;
import java.time.Instant;

public record TutorResponse(Long id, String nome, String cpf, String telefone, String email,
                            String endereco, Instant dataCadastro) {
    public static TutorResponse from(Tutor tutor) {
        return new TutorResponse(tutor.getId(), tutor.getNome(), tutor.getCpf(), tutor.getTelefone(),
                tutor.getEmail(), tutor.getEndereco(), tutor.getDataCadastro());
    }
}
