package ipiranga.fatec.qrvet.integration;

import ipiranga.fatec.qrvet.models.AuditLog;
import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.models.enums.Role;
import ipiranga.fatec.qrvet.repositories.AuditLogRepository;
import ipiranga.fatec.qrvet.repositories.UserRepository;
import ipiranga.fatec.qrvet.security.SessionService;
import ipiranga.fatec.qrvet.security.TokenService;
import jakarta.servlet.http.Cookie;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditFlowIntegrationTests {
    private static final String PASSWORD = "Test-only-123!";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired AuditLogRepository auditLogs;
    @Autowired TokenService tokens;
    @Autowired PasswordEncoder passwords;
    @MockitoBean SessionService sessions;
    @MockitoBean StringRedisTemplate redis;
    @MockitoBean JavaMailSender mail;

    private final JsonMapper json = JsonMapper.builder().build();
    private User admin;
    private User receptionist;
    private User veterinarian;

    @BeforeEach
    void setup() {
        auditLogs.deleteAll();
        admin = users.findByEmailIgnoreCase("admin@qrvet.test").orElseThrow();
        receptionist = user(Role.RECEPCIONISTA);
        veterinarian = user(Role.VETERINARIO);
    }

    @Test
    void loginCreatesAuditRecordWithoutPassword() throws Exception {
        perform(post("/v1/auth/login").content("""
                {"email":"%s","password":"%s"}
                """.formatted(receptionist.getEmail(), PASSWORD)), null)
                .andExpect(status().isOk());

        AuditLog audit = await("LOGIN_SUCCESS");
        assertFalse(audit.getDetails().contains(PASSWORD));
        assertTrue(audit.getDetails().contains(receptionist.getEmail()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void recoveryAndResetFlowsDoNotExposeTokenOrPasswordInAuditData() throws Exception {
        ValueOperations<String, String> values = Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        perform(post("/v1/auth/forgot-password").content("""
                {"email":"%s"}
                """.formatted(receptionist.getEmail())), null)
                .andExpect(status().isAccepted());

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail, timeout(2000)).send(message.capture());
        String emailBody = message.getValue().getText();
        assertNotNull(emailBody);
        String recoveryToken = emailBody.substring(emailBody.lastIndexOf("#token=") + "#token=".length())
                .split("\\R", 2)[0]
                .trim();
        assertFalse(recoveryToken.isBlank());

        AuditLog requested = await("PASSWORD_RECOVERY_REQUESTED");
        assertFalse(requested.getDetails().contains(recoveryToken));
        assertFalse(requested.getDetails().contains(PASSWORD));
        assertFalse(requested.getDetails().toLowerCase().contains("secret"));
        assertFalse(requested.getDetails().toLowerCase().contains("credential"));

        when(values.getAndDelete("password-recovery:" + recoveryToken))
                .thenReturn(receptionist.getId() + ":0");
        perform(post("/v1/auth/reset-password").content("""
                {"token":"%s","newPassword":"Reset-only-456!"}
                """.formatted(recoveryToken)), null)
                .andExpect(status().isNoContent());

        AuditLog reset = await("PASSWORD_RESET");
        assertTrue(reset.getDetails() == null || !reset.getDetails().contains(recoveryToken));
        assertTrue(reset.getDetails() == null || !reset.getDetails().contains("Reset-only-456!"));
    }

    @Test
    void failedLoginDoesNotPersistPasswordOrCredentialValues() throws Exception {
        String password = "wrong-password-value";

        perform(post("/v1/auth/login").content("""
                {"email":"%s","password":"%s"}
                """.formatted(receptionist.getEmail(), password)), null)
                .andExpect(status().isInternalServerError());

        AuditLog failed = await("LOGIN_FAILED");
        assertFalse(failed.getDetails().contains(password));
        assertFalse(failed.getDetails().toLowerCase().contains("credential"));
        assertFalse(failed.getDetails().toLowerCase().contains("secret"));
    }

    @Test
    void patientAndHospitalizationFlowsCreateAuditRecords() throws Exception {
        long tutorId = body(perform(post("/v1/tutores").content("""
                {"nome":"Ana Silva","cpf":"52998224725","telefone":"11999999999","email":"audit@qrvet.test","endereco":"Rua A"}
                """), receptionist).andExpect(status().isCreated())).get("id").asLong();
        long patientId = body(perform(post("/v1/pacientes").content("""
                {"tutorId":%d,"nome":"Luna","especie":"Felina","raca":"SRD","sexo":"Fêmea","peso":4.5,"dataNascimento":"2021-04-12","observacoes":"Observação"}
                """.formatted(tutorId)), receptionist).andExpect(status().isCreated())).get("id").asLong();
        assertTrue(awaitAction("PATIENT_CREATED", patientId));

        perform(put("/v1/pacientes/" + patientId).content("""
                {"tutorId":%d,"nome":"Luna Atualizada","especie":"Felina","raca":"SRD","sexo":"Fêmea","peso":4.7,"dataNascimento":"2021-04-12","observacoes":"Atualizada"}
                """.formatted(tutorId)), receptionist).andExpect(status().isOk());
        assertTrue(awaitAction("PATIENT_UPDATED", patientId));

        long bayId = body(perform(post("/v1/baias").content("""
                {"identificacao":"AUDIT-%s"}
                """.formatted(UUID.randomUUID())), admin).andExpect(status().isCreated())).get("id").asLong();
        long hospitalizationId = body(perform(post("/v1/internacoes").content("""
                {"pacienteId":%d,"baiaId":%d,"veterinarioId":%d,"motivo":"Observação","diagnosticoInicial":"Avaliação","observacoes":""}
                """.formatted(patientId, bayId, veterinarian.getId())), receptionist)
                .andExpect(status().isCreated())).get("id").asLong();
        assertTrue(awaitAction("HOSPITALIZATION_CREATED", hospitalizationId));

        perform(put("/v1/internacoes/" + hospitalizationId + "/encerrar")
                .content("{\"statusEncerramento\":\"ALTA\"}"), receptionist)
                .andExpect(status().isOk());
        assertTrue(awaitAction("HOSPITALIZATION_UPDATED", hospitalizationId));
    }

    private User user(Role role) {
        return users.saveAndFlush(User.builder()
                .name("Audit " + role)
                .email(UUID.randomUUID() + "@qrvet.test")
                .role(role)
                .active(true)
                .confirmed(true)
                .passwordHash(passwords.encode(PASSWORD))
                .build());
    }

    private AuditLog await(String action) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        while (System.nanoTime() < deadline) {
            List<AuditLog> matches = auditLogs.findAll().stream()
                    .filter(log -> action.equals(log.getAction()))
                    .toList();
            if (!matches.isEmpty()) {
                return matches.get(0);
            }
            Thread.sleep(25);
        }
        throw new AssertionError("Audit action was not persisted: " + action);
    }

    private boolean awaitAction(String action, Long resourceId) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        while (System.nanoTime() < deadline) {
            if (auditLogs.findAll().stream().anyMatch(log ->
                    action.equals(log.getAction()) && String.valueOf(resourceId).equals(log.getResourceId()))) {
                return true;
            }
            Thread.sleep(25);
        }
        return false;
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, User user) throws Exception {
        if (user != null) {
            request.header("Authorization", "Bearer " + tokens.generateAccessToken(user, Instant.now()));
        }
        return mvc.perform(request
                .cookie(new Cookie("XSRF-TOKEN", "test-csrf"))
                .header("X-XSRF-TOKEN", "test-csrf")
                .contentType("application/json"));
    }
}
