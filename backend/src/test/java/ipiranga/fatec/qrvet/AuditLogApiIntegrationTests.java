package ipiranga.fatec.qrvet;

import ipiranga.fatec.qrvet.models.AuditLog;
import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.models.enums.Role;
import ipiranga.fatec.qrvet.repositories.AuditLogRepository;
import ipiranga.fatec.qrvet.repositories.UserRepository;
import ipiranga.fatec.qrvet.security.SessionService;
import ipiranga.fatec.qrvet.security.TokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuditLogApiIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired AuditLogRepository auditLogs;
    @Autowired TokenService tokens;
    @Autowired PasswordEncoder passwords;
    @MockitoBean SessionService sessions;
    @MockitoBean StringRedisTemplate redis;
    @MockitoBean JavaMailSender mail;

    private User admin;
    private User receptionist;

    @BeforeEach
    void setup() {
        admin = users.findByEmailIgnoreCase("admin@qrvet.test").orElseThrow();
        receptionist = users.saveAndFlush(User.builder()
                .name("Audit test receptionist")
                .email(UUID.randomUUID() + "@qrvet.test")
                .role(Role.RECEPCIONISTA)
                .active(true)
                .confirmed(true)
                .passwordHash(passwords.encode("Test-only-123!"))
                .build());
    }

    @Test
    void requiresAuthenticationAndAdminRole() throws Exception {
        mvc.perform(get("/v1/audit-logs"))
                .andExpect(status().isUnauthorized());

        perform(get("/v1/audit-logs"), receptionist)
                .andExpect(status().isForbidden());
    }

    @Test
    void adminReceivesPaginatedFilteredLogs() throws Exception {
        Instant first = Instant.parse("2026-01-10T10:00:00Z");
        save("LOGIN_SUCCESS", "auth", admin.getId(), first, "{\"email\":\"admin@qrvet.test\"}");
        save("PATIENT_CREATED", "patient", receptionist.getId(), first.plusSeconds(1), "{\"name\":\"Luna\"}");
        save("PATIENT_UPDATED", "patient", receptionist.getId(), first.plusSeconds(2), "{\"name\":\"Mia\"}");
        save("INTERNACAO_UPDATED", "internacao", admin.getId(), first.plusSeconds(3), "{\"status\":\"ALTA\"}");

        perform(get("/v1/audit-logs")
                        .param("page", "0")
                        .param("size", "1")
                        .param("action", "patient")
                        .param("resourceName", "PATIENT")
                        .param("userId", receptionist.getId().toString())
                        .param("startDate", "2026-01-10T10:00:00Z")
                        .param("endDate", "2026-01-10T10:00:02Z"), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.pages").value(2))
                .andExpect(jsonPath("$.items[0].action").value("Paciente atualizado"))
                .andExpect(jsonPath("$.items[0].resourceName").value("paciente"));
    }

    @Test
    void rejectsInvalidPagination() throws Exception {
        perform(get("/v1/audit-logs").param("page", "-1").param("size", "101"), admin)
                .andExpect(status().isBadRequest());
    }

    private void save(String action, String resourceName, Long userId, Instant timestamp, String details) {
        AuditLog log = new AuditLog();
        log.setTimestamp(timestamp);
        log.setUserId(userId);
        log.setUserEmail(userId.equals(admin.getId()) ? admin.getEmail() : receptionist.getEmail());
        log.setIpAddress("127.0.0.1");
        log.setAction(action);
        log.setResourceName(resourceName);
        log.setResourceId(String.valueOf(userId));
        log.setDetails(details);
        auditLogs.saveAndFlush(log);
    }

    private ResultActions perform(
            MockHttpServletRequestBuilder request, User user) throws Exception {
        return mvc.perform(request
                .header("Authorization", "Bearer " + tokens.generateAccessToken(user, Instant.now()))
                .cookie(new Cookie("XSRF-TOKEN", "test-csrf"))
                .header("X-XSRF-TOKEN", "test-csrf")
                .contentType("application/json"));
    }
}
