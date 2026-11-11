package ipiranga.fatec.qrvet.models.enums;

import java.util.Arrays;

public enum AuditAction {
    LOGIN_SUCCESS("LOGIN_SUCCESS", "Login realizado"),
    LOGIN_FAILED("LOGIN_FAILED", "Falha no login"),
    LOGOUT("LOGOUT", "Logout realizado"),
    PASSWORD_CHANGED("PASSWORD_CHANGED", "Senha alterada"),
    PASSWORD_RECOVERY_REQUESTED("PASSWORD_RECOVERY_REQUESTED", "Recuperação de senha solicitada"),
    PASSWORD_RESET("PASSWORD_RESET", "Senha redefinida"),
    PATIENT_CREATED("PATIENT_CREATED", "Paciente criado"),
    PATIENT_UPDATED("PATIENT_UPDATED", "Paciente atualizado"),
    PATIENT_ALERTS_UPDATED("PATIENT_ALERTS_UPDATED", "Alertas do paciente atualizados"),
    PROFILE_UPDATED("PROFILE_UPDATED", "Perfil atualizado"),
    EVOLUTION_CREATED("EVOLUTION_CREATED", "Evolução registrada"),
    HOSPITALIZATION_CREATED("HOSPITALIZATION_CREATED", "Internação criada"),
    HOSPITALIZATION_UPDATED("HOSPITALIZATION_UPDATED", "Internação atualizada");

    private final String code;
    private final String label;

    AuditAction(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static String labelFor(String code) {
        return Arrays.stream(values())
                .filter(action -> action.code.equals(code))
                .map(AuditAction::label)
                .findFirst()
                .orElse(code);
    }
}
