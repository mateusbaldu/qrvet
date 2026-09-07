package ipiranga.fatec.qrvet.service;

import ipiranga.fatec.qrvet.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {
    private final JavaMailSender sender;
    private final String from;
    private final String frontendUrl;

    public MailService(JavaMailSender sender, @Value("${app.mail.from}") String from,
                       @Value("${app.frontend-url}") String frontendUrl) {
        this.sender = sender;
        this.from = from;
        this.frontendUrl = frontendUrl;
    }

    public void sendPasswordReset(String to, String token) {
        send(to, "QRVet — recuperação de senha",
            "Recebemos uma solicitação para redefinir sua senha.\n\n" +
            "Abra este link: " + frontendUrl + "/redefinir-senha?token=" + token + "\n\n" +
            "O link expira em 30 minutos e pode ser usado uma única vez.");
    }

    public void sendInvitation(String to, String token, String clinicName) {
        send(to, "QRVet — convite para " + clinicName,
            "Você foi convidado para fazer parte da equipe de " + clinicName + ".\n\n" +
            "Abra este link para criar sua senha: " + frontendUrl + "/aceitar-convite?token=" + token + "\n\n" +
            "O convite expira em 7 dias.");
    }

    private void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            sender.send(message);
        } catch (MailException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "EMAIL_DELIVERY_FAILED",
                "Não foi possível enviar o e-mail agora.");
        }
    }
}
