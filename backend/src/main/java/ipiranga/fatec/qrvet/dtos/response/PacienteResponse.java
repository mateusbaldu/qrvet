package ipiranga.fatec.qrvet.dtos.response;

import ipiranga.fatec.qrvet.models.Paciente;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PacienteResponse(Long id, Long tutorId, String nome, String especie, String raca,
                               String sexo, LocalDate dataNascimento, BigDecimal peso,
                               String observacoes, Instant dataCadastro) {
    public static PacienteResponse from(Paciente paciente) {
        return new PacienteResponse(paciente.getId(), paciente.getTutor().getId(), paciente.getNome(),
                paciente.getEspecie(), paciente.getRaca(), paciente.getSexo(), paciente.getDataNascimento(),
                paciente.getPeso(), paciente.getObservacoes(), paciente.getDataCadastro());
    }
}
