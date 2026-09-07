package ipiranga.fatec.qrvet.event;

public record PasswordRecoveryEmailRequested(
        String recipient,
        String token) {
}
