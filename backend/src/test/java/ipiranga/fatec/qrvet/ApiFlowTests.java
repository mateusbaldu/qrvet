package ipiranga.fatec.qrvet;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ipiranga.fatec.qrvet.model.Clinic;
import ipiranga.fatec.qrvet.model.Role;
import ipiranga.fatec.qrvet.model.UserAccount;
import ipiranga.fatec.qrvet.repository.ClinicRepository;
import ipiranga.fatec.qrvet.repository.PasswordResetTokenRepository;
import ipiranga.fatec.qrvet.repository.RefreshSessionRepository;
import ipiranga.fatec.qrvet.repository.TeamInvitationRepository;
import ipiranga.fatec.qrvet.repository.UserAccountRepository;
import ipiranga.fatec.qrvet.service.MailService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiFlowTests {
    private static final Pattern ACCESS_TOKEN = Pattern.compile("\\\"accessToken\\\":\\\"([^\\\"]+)\\\"");

    @Autowired MockMvc mvc;
    @Autowired ClinicRepository clinics;
    @Autowired UserAccountRepository users;
    @Autowired TeamInvitationRepository invitations;
    @Autowired PasswordResetTokenRepository resetTokens;
    @Autowired RefreshSessionRepository sessions;
    @Autowired PasswordEncoder encoder;
    @MockitoBean MailService mailService;

    @BeforeEach
    void seed() {
        sessions.deleteAll();
        resetTokens.deleteAll();
        invitations.deleteAll();
        users.deleteAll();
        clinics.deleteAll();
        Clinic clinic = clinics.save(new Clinic("Clínica Teste"));
        users.save(new UserAccount(clinic, "Admin Teste", "admin@qrvet.test",
            encoder.encode("Qrvet@123"), Role.ADMIN));
    }

    @Test
    void loginAndReadTeam() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@qrvet.test\",\"password\":\"Qrvet@123\"}"))
            .andExpect(status().isOk())
            .andExpect(cookie().httpOnly("qrvet_refresh", true))
            .andExpect(jsonPath("$.user.role").value("ADMIN"))
            .andReturn();

        Matcher matcher = ACCESS_TOKEN.matcher(login.getResponse().getContentAsString());
        if (!matcher.find()) throw new AssertionError("Access token não retornado");

        mvc.perform(get("/api/team/members").header("Authorization", "Bearer " + matcher.group(1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].email").value("admin@qrvet.test"))
            .andExpect(jsonPath("$.items[0].status").value("ONLINE"))
            .andExpect(jsonPath("$.summary.roles.ADMIN").value(1));
    }

    @Test
    void refreshCookieRestoresSessionAfterPageReload() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@qrvet.test\",\"password\":\"Qrvet@123\"}"))
            .andExpect(status().isOk())
            .andReturn();

        Cookie refreshCookie = login.getResponse().getCookie("qrvet_refresh");
        if (refreshCookie == null) throw new AssertionError("Refresh cookie não retornado");

        mvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
            .andExpect(status().isOk())
            .andExpect(cookie().httpOnly("qrvet_refresh", true))
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.user.email").value("admin@qrvet.test"));
    }

    @Test
    void passwordRecoveryUsesNeutralResponseAndSendsMail() throws Exception {
        mvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@qrvet.test\"}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.message").exists());

        verify(mailService).sendPasswordReset(anyString(), anyString());
    }
}
