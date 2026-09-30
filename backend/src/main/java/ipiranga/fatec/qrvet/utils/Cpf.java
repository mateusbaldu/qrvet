package ipiranga.fatec.qrvet.utils;

public final class Cpf {

    private Cpf() {}

    public static String normalize(String cpf) {
        if (cpf == null) {
            return "";
        }
        return cpf.replaceAll("\\D", "");
    }

    public static boolean isValid(String cpf) {
        if (cpf == null) {
            return false;
        }
        String normalized = normalize(cpf);
        if (normalized.length() != 11 || normalized.chars().distinct().count() == 1) {
            return false;
        }
        int first = checkDigit(normalized, 9);
        int second = checkDigit(normalized, 10);
        return first == normalized.charAt(9) - '0' && second == normalized.charAt(10) - '0';
    }

    private static int checkDigit(String cpf, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (cpf.charAt(i) - '0') * (length + 1 - i);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
