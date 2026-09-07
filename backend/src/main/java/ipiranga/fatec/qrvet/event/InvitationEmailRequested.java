package ipiranga.fatec.qrvet.event;

public record InvitationEmailRequested(
        String recipient,
        String recipientName,
        String token) {
}
