package ipiranga.fatec.qrvet.common;

public final class PasswordPolicy {
    private PasswordPolicy() {}

    public static boolean isValid(String value) {
        return value != null
            && value.length() >= 8
            && value.matches(".*[A-Z].*")
            && value.matches(".*[a-z].*")
            && value.matches(".*\\d.*")
            && value.matches(".*[^A-Za-z0-9].*");
    }

    public static void requireValid(String value) {
        if (!isValid(value)) {
            throw new ApiException(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                "WEAK_PASSWORD", "A senha não atende aos requisitos de segurança.");
        }
    }
}
