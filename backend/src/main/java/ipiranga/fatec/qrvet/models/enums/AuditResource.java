package ipiranga.fatec.qrvet.models.enums;

import java.util.Arrays;

public enum AuditResource {
    AUTH("auth", "autenticação"),
    PATIENT("patient", "paciente"),
    HOSPITALIZATION("hospitalization", "internação");

    private final String code;
    private final String label;

    AuditResource(String code, String label) {
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
                .filter(resource -> resource.code.equals(code))
                .map(AuditResource::label)
                .findFirst()
                .orElse(code);
    }
}
