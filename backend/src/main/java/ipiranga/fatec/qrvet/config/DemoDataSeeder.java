package ipiranga.fatec.qrvet.config;

import ipiranga.fatec.qrvet.common.TokenTools;
import ipiranga.fatec.qrvet.model.Clinic;
import ipiranga.fatec.qrvet.model.Role;
import ipiranga.fatec.qrvet.model.TeamInvitation;
import ipiranga.fatec.qrvet.model.UserAccount;
import ipiranga.fatec.qrvet.repository.ClinicRepository;
import ipiranga.fatec.qrvet.repository.TeamInvitationRepository;
import ipiranga.fatec.qrvet.repository.UserAccountRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {
    private final ClinicRepository clinics;
    private final UserAccountRepository users;
    private final TeamInvitationRepository invitations;
    private final PasswordEncoder encoder;

    public DemoDataSeeder(ClinicRepository clinics, UserAccountRepository users,
                          TeamInvitationRepository invitations, PasswordEncoder encoder) {
        this.clinics = clinics;
        this.users = users;
        this.invitations = invitations;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) return;
        Clinic clinic = clinics.save(new Clinic("Clínica QRVet"));
        create(clinic, "Dra. Sarah Jenkins", "sarah@qrvet.clinic", Role.ADMIN,
            "(11) 99999-1234", "CRMV-SP 12345");
        create(clinic, "Mike Ross", "mike@qrvet.clinic", Role.VETERINARIAN, null, "CRMV-SP 23456");
        UserAccount anna = create(clinic, "Anna Costa", "anna@qrvet.clinic", Role.VETERINARIAN,
            null, "CRMV-SP 34567");
        anna.deactivate();
        users.save(anna);
        create(clinic, "Camila Alves", "camila@qrvet.clinic", Role.RECEPTIONIST, null, null);

        String invitationToken = TokenTools.randomToken();
        invitations.save(new TeamInvitation(clinic, "lucas@qrvet.clinic", Role.VETERINARIAN,
            TokenTools.sha256(invitationToken), Instant.now().plus(Duration.ofDays(7))));
    }

    private UserAccount create(Clinic clinic, String name, String email, Role role, String phone, String crmv) {
        UserAccount user = new UserAccount(clinic, name, email, encoder.encode("Qrvet@123"), role);
        user.updateProfile(name, email, phone, crmv);
        return users.save(user);
    }
}
