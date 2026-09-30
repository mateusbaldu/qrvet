package ipiranga.fatec.qrvet;

import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.models.enums.Role;
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
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ClinicIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired TokenService tokens;
    @Autowired PasswordEncoder passwords;
    @MockitoBean SessionService sessions;
    @MockitoBean StringRedisTemplate redis;
    @MockitoBean JavaMailSender mail;
    private final JsonMapper json = JsonMapper.builder().build();
    private User admin;
    private User vet;
    private User receptionist;
    private User assistant;

    @BeforeEach
    void setup() {
        admin = users.findByEmailIgnoreCase("admin@qrvet.test").orElseThrow();
        vet = user(Role.VETERINARIO, true);
        receptionist = user(Role.RECEPCIONISTA, true);
        assistant = user(Role.AUXILIAR_TECNICO, true);
    }

    private User user(Role role, boolean active) {
        return users.saveAndFlush(User.builder().name("Teste " + role).email(UUID.randomUUID() + "@qrvet.test")
                .role(role).active(active).confirmed(active).passwordHash(passwords.encode("Test-only-123!")).build());
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, User user) throws Exception {
        if (user != null) request.header("Authorization", "Bearer " + tokens.generateAccessToken(user, Instant.now()));
        return mvc.perform(request.cookie(new Cookie("XSRF-TOKEN", "test-csrf"))
                .header("X-XSRF-TOKEN", "test-csrf").contentType("application/json"));
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private JsonNode openHospitalization() throws Exception {
        long tutor = body(perform(post("/v1/tutores").content("""
                {"nome":"Ana Silva","cpf":"52998224725","telefone":"11999999999","email":"ana@qrvet.test","endereco":"Rua A"}
                """), receptionist).andExpect(status().isCreated())).get("id").asLong();
        long patient = body(perform(post("/v1/pacientes").content("""
                {"tutorId":%d,"nome":"Luna","especie":"Felina","raca":"SRD","sexo":"Fêmea","peso":4.5,"dataNascimento":"2021-04-12","observacoes":"Sensível a ruídos"}
                """.formatted(tutor)), receptionist).andExpect(status().isCreated())).get("id").asLong();
        long bay = body(perform(post("/v1/baias").content("{\"identificacao\":\"B-" + UUID.randomUUID() + "\"}"), admin)
                .andExpect(status().isCreated())).get("id").asLong();
        return body(perform(post("/v1/internacoes").content("""
                {"pacienteId":%d,"baiaId":%d,"veterinarioId":%d,"motivo":"Observação","diagnosticoInicial":"Avaliação","observacoes":""}
                """.formatted(patient, bay, vet.getId())), receptionist).andExpect(status().isCreated()));
    }

    @Test
    void authenticatedReadsKeepCsrfCookieAndCookieEndpointsStillRequireIt() throws Exception {
        perform(get("/v1/auth/me"), admin).andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("XSRF-TOKEN"));
        mvc.perform(post("/v1/auth/refresh"))
                .andExpect(status().isForbidden());
        perform(post("/v1/auth/heartbeat"), admin).andExpect(status().isNoContent());
    }

    @Test
    void patientAdmissionCareDischargeAndPublicQrUseTheSameRecords() throws Exception {
        var admission = openHospitalization();
        long id = admission.get("id").asLong();
        long patientId = admission.get("pacienteId").asLong();
        assertEquals("Luna", admission.get("pacienteNome").asText());
        assertEquals(vet.getName(), admission.get("veterinarioNome").asText());
        perform(get("/v1/pacientes/" + patientId), receptionist).andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Luna"));
        perform(get("/v1/internacoes").param("pacienteId", String.valueOf(patientId)), receptionist)
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        perform(post("/v1/internacoes/" + id + "/jejum").content("{\"motivo\":\"Exame\"}"), assistant).andExpect(status().isCreated());
        perform(post("/v1/internacoes/" + id + "/alimentacao").content("{\"alimento\":\"Ração\",\"quantidade\":\"40 g\"}"), assistant).andExpect(status().isBadRequest());
        String publicPath = "/v1/public/internacoes/qr/" + admission.get("uuidToken").asText();
        perform(get(publicPath), null).andExpect(status().isOk()).andExpect(jsonPath("$.jejumAtivo").value(true)).andExpect(jsonPath("$.tutorId").doesNotExist());
        perform(put("/v1/internacoes/" + id + "/jejum/encerrar"), assistant).andExpect(status().isOk());
        perform(post("/v1/internacoes/" + id + "/alimentacao").content("{\"alimento\":\"Ração\",\"quantidade\":\"40 g\",\"aceitacaoObservacao\":\"Boa\"}"), assistant).andExpect(status().isCreated());
        perform(get("/v1/internacoes/" + id + "/alimentacao"), vet).andExpect(jsonPath("$[0].quantidade").value("40 g"));
        perform(post("/v1/internacoes/" + id + "/jejum").content("{\"motivo\":\"Outro exame\"}"), vet).andExpect(status().isCreated());
        perform(put("/v1/internacoes/" + id + "/encerrar").content("{\"statusEncerramento\":\"ALTA\"}"), receptionist).andExpect(status().isOk());
        perform(get(publicPath), null).andExpect(jsonPath("$.status").value("ALTA")).andExpect(jsonPath("$.jejumAtivo").value(false));
        perform(get("/v1/baias/" + admission.get("baiaId").asLong()), receptionist).andExpect(jsonPath("$.status").value("DISPONIVEL"));
        perform(get("/v1/internacoes/" + id + "/cuidados"), assistant).andExpect(jsonPath("$.status").value("ALTA"));
        perform(post("/v1/internacoes/" + id + "/alimentacao").content("{\"alimento\":\"Ração\",\"quantidade\":\"40 g\"}"), assistant).andExpect(status().isBadRequest());
        perform(get("/v1/internacoes/" + id + "/qrcode"), receptionist).andExpect(status().isOk()).andExpect(jsonPath("$.base64").isNotEmpty());
    }

    @Test
    void careDirectoryDoesNotGrantAccessToPrivatePatientDataOrAdministration() throws Exception {
        var admission = openHospitalization();
        long id = admission.get("id").asLong();
        perform(get("/v1/internacoes/cuidados"), assistant).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].pacienteNome").value("Luna")).andExpect(jsonPath("$.items[0].diagnosticoInicial").doesNotExist());
        perform(get("/v1/internacoes/" + id), assistant).andExpect(status().isForbidden());
        perform(get("/v1/pacientes"), assistant).andExpect(status().isForbidden());
        perform(get("/v1/internacoes/" + id + "/jejum"), receptionist).andExpect(status().isForbidden());
        perform(get("/v1/users"), receptionist).andExpect(status().isForbidden());
        perform(get("/v1/internacoes/veterinarios"), receptionist).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value(vet.getName())).andExpect(jsonPath("$[0].email").doesNotExist());
        perform(get("/v1/internacoes/cuidados"), user(Role.TUTOR, true)).andExpect(status().isForbidden());
    }

    @Test
    @SuppressWarnings("unchecked")
    void invitationCanBeAcceptedWithoutAnAdministratorSession() throws Exception {
        var pending = user(Role.VETERINARIO, false);
        String token = UUID.randomUUID().toString();
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.getAndDelete("invitation:" + token)).thenReturn(pending.getId().toString());
        perform(post("/v1/auth/confirm-invitation").content("{\"token\":\"" + token + "\",\"newPassword\":\"Welcome-123!\"}"), null).andExpect(status().isNoContent());
        var accepted = users.findById(pending.getId()).orElseThrow();
        assertTrue(accepted.isConfirmed());
        assertTrue(accepted.isActive());
        assertTrue(accepted.passwordMatches("Welcome-123!", passwords));
    }

    @Test
    void duplicateAdmissionsAndInvalidFormsAreRejected() throws Exception {
        var admission = openHospitalization();
        perform(post("/v1/internacoes").content("""
                {"pacienteId":%d,"baiaId":%d,"veterinarioId":%d,"motivo":"Novo","diagnosticoInicial":"Teste"}
                """.formatted(admission.get("pacienteId").asLong(), admission.get("baiaId").asLong(), vet.getId())), admin).andExpect(status().isConflict());
        perform(post("/v1/pacientes").content("{\"nome\":\"\"}"), admin).andExpect(status().isBadRequest());
        perform(get("/v1/pacientes/9999999"), vet).andExpect(status().isNotFound());
    }
}
