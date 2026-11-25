package ipiranga.fatec.qrvet.dtos.response;

import ipiranga.fatec.qrvet.models.Paciente;

public record AlertasPacienteResponse(String alergias, String restricoesAlimentares, String cuidadosManejo) {
    public static AlertasPacienteResponse from(Paciente paciente) {
        return new AlertasPacienteResponse(paciente.getAlergias(), paciente.getRestricoesAlimentares(), paciente.getCuidadosManejo());
    }
}
