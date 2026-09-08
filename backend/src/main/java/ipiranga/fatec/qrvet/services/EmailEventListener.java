package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.event.InvitationEmailRequested;
import ipiranga.fatec.qrvet.event.PasswordRecoveryEmailRequested;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailEventListener {
    private final JavaMailSender mail;
    private final String frontend;
    private final String sender;

    public EmailEventListener(
            JavaMailSender mail,
            @Value("${qrvet.frontend-url}") String frontend,
            @Value("${qrvet.mail-from}") String sender) {
        this.mail = mail;
        this.frontend = frontend;
        this.sender = sender;
    }

    @EventListener
    public void sendInvitation(InvitationEmailRequested event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(event.recipient());
        message.setSubject("Você foi convidado para o QRVet");
        message.setText(
                "Olá, "
                        + event.recipientName()
                        + "!\n\nO administrador convidou você para fazer parte da equipe QRVet.\n"
                        + "Confirme seu e-mail e escolha sua senha pelo link:\n\n"
                        + frontend.replaceAll("/+$", "")
                        + "/registro#token="
                        + event.token()
                        + "\n\nO convite é válido por 24 horas e pode ser usado uma única vez.");
        mail.send(message);
    }

    @EventListener
    public void sendPasswordRecovery(PasswordRecoveryEmailRequested event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(event.recipient());
        message.setSubject("QRVet - Redefinição de Senha");
        message.setText(
                "Recebemos uma solicitação para redefinir sua senha no QRVet.\n\n"
                        + frontend.replaceAll("/+$", "")
                        + "/reset-password#token="
                        + event.token()
                        + "\n\nEste link fica válido por 20 minutos e pode ser usado uma única vez.");
        mail.send(message);
    }
}
