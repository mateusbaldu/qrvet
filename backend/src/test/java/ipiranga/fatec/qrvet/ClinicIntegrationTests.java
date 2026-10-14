package ipiranga.fatec.qrvet;

import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.models.enums.Role;
import ipiranga.fatec.qrvet.repositories.UserRepository;
import ipiranga.fatec.qrvet.repositories.TutorRepository;
import ipiranga.fatec.qrvet.security.SessionService;
import ipiranga.fatec.qrvet.security.TokenService;
import jakarta.servlet.http.Cookie;
import jakarta.persistence.EntityManager;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
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
@ExtendWith(OutputCaptureExtension.class)
class ClinicIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired TokenService tokens;
    @Autowired PasswordEncoder passwords;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entities;
    @Autowired TutorRepository tutors;
    private static final Path photoDirectory = temporaryPhotoDirectory();
    private static Path temporaryPhotoDirectory() {
        try { return Files.createTempDirectory("qrvet-photo-tests-"); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
    @DynamicPropertySource
    static void photos(DynamicPropertyRegistry properties) {
        properties.add("qrvet.upload-dir", photoDirectory::toString);
    }
    @AfterAll
    static void cleanupPhotos() throws Exception {
        try (Stream<Path> files = Files.list(photoDirectory)) {
            for (Path file : files.toList()) Files.delete(file);
        }
        Files.delete(photoDirectory);
    }
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
        JsonNode admission = openHospitalization();
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
    void hospitalizationLogsOperationStartAndCompletionAtInfoWithoutSensitiveData(
            CapturedOutput output) throws Exception {
        JsonNode admission = openHospitalization();
        long patientId = admission.get("pacienteId").asLong();
        long hospitalizationId = admission.get("id").asLong();
        long bayId = admission.get("baiaId").asLong();

        assertTrue(output.getOut().contains("INFO"));
        assertTrue(output.getOut().contains(
                "Opening hospitalization for patient " + patientId + " in bay " + bayId));
        assertTrue(output.getOut().contains(
                "Hospitalization " + hospitalizationId + " opened for patient " + patientId));
        assertFalse(output.getOut().contains("Test-only-123!"));
        assertFalse(output.getOut().toLowerCase().contains("password"));
        assertFalse(output.getOut().toLowerCase().contains("credential"));
    }

    @Test
    void careDirectoryDoesNotGrantAccessToPrivatePatientDataOrAdministration() throws Exception {
        JsonNode admission = openHospitalization();
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
    void printedQrOpensTheStaffAdmissionAndKeepsTutorAccessSeparate() throws Exception {
        JsonNode admission = openHospitalization();
        long id = admission.get("id").asLong();
        JsonNode qr = body(perform(get("/v1/internacoes/" + id + "/qrcode"), receptionist)
                .andExpect(status().isOk()));
        String staffUrl = "http://localhost:5173/internacoes/" + id;
        String tutorUrl = "http://localhost:5173/public/internacoes/qr/" + admission.get("uuidToken").asText();
        assertEquals(staffUrl, qr.get("url").asText());
        assertEquals(tutorUrl, qr.get("tutorUrl").asText());
        assertEquals("ana@qrvet.test", qr.get("tutorEmail").asText());
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(
                Base64.getDecoder().decode(qr.get("base64").asText())));
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(
                new BufferedImageLuminanceSource(image)));
        assertEquals(staffUrl, new QRCodeReader().decode(bitmap).getText());
        perform(get("/v1/internacoes/" + id), null).andExpect(status().isUnauthorized());
        perform(get("/v1/public/internacoes/qr/" + admission.get("uuidToken").asText()), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.pacienteNome").value("Luna"))
                .andExpect(jsonPath("$.tutorEmail").doesNotExist());
        verifyNoInteractions(mail);
    }

    @Test
    void staffCanEmailThePublicLinkToTheRegisteredTutor() throws Exception {
        JsonNode admission = openHospitalization();
        long id = admission.get("id").asLong();
        for (User sender : List.of(admin, vet, receptionist)) {
            perform(post("/v1/internacoes/" + id + "/tutor/email"), sender)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Link de acompanhamento enviado para ana@qrvet.test."));
        }
        ArgumentCaptor<SimpleMailMessage> messages = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail, times(3)).send(messages.capture());
        for (SimpleMailMessage message : messages.getAllValues()) {
            assertArrayEquals(new String[] { "ana@qrvet.test" }, message.getTo());
            assertTrue(message.getSubject().contains("Luna"));
            assertTrue(message.getText().contains("Ana Silva"));
            assertTrue(message.getText().contains("http://localhost:5173/public/internacoes/qr/"
                    + admission.get("uuidToken").asText()));
            assertFalse(message.getText().contains("http://localhost:5173/internacoes/"));
        }
    }

    @Test
    void tutorEmailRequiresStaffPermissionAndReportsMailFailure() throws Exception {
        JsonNode admission = openHospitalization();
        String path = "/v1/internacoes/" + admission.get("id").asLong() + "/tutor/email";
        perform(post(path), null).andExpect(status().isUnauthorized());
        perform(post(path), assistant).andExpect(status().isForbidden());
        perform(post(path), user(Role.TUTOR, true)).andExpect(status().isForbidden());
        perform(post("/v1/internacoes/9999999/tutor/email"), receptionist).andExpect(status().isNotFound());
        verifyNoInteractions(mail);
        doThrow(new MailSendException("SMTP unavailable"))
                .when(mail).send(any(SimpleMailMessage.class));
        perform(post(path), receptionist).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value(
                        "Não foi possível enviar o e-mail. Confira a configuração de e-mail da clínica e tente novamente."));
    }

    @Test
    @SuppressWarnings("unchecked")
    void invitationCanBeAcceptedWithoutAnAdministratorSession() throws Exception {
        User pending = user(Role.VETERINARIO, false);
        String token = UUID.randomUUID().toString();
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.getAndDelete("invitation:" + token)).thenReturn(pending.getId().toString());
        perform(post("/v1/auth/confirm-invitation").content("{\"token\":\"" + token + "\",\"newPassword\":\"Welcome-123!\"}"), null).andExpect(status().isNoContent());
        User accepted = users.findById(pending.getId()).orElseThrow();
        assertTrue(accepted.isConfirmed());
        assertTrue(accepted.isActive());
        assertTrue(accepted.passwordMatches("Welcome-123!", passwords));
    }

    @Test
    void duplicateAdmissionsAndInvalidFormsAreRejected() throws Exception {
        JsonNode admission = openHospitalization();
        perform(post("/v1/internacoes").content("""
                {"pacienteId":%d,"baiaId":%d,"veterinarioId":%d,"motivo":"Novo","diagnosticoInicial":"Teste"}
                """.formatted(admission.get("pacienteId").asLong(), admission.get("baiaId").asLong(), vet.getId())), admin).andExpect(status().isConflict());
        perform(post("/v1/pacientes").content("{\"nome\":\"\"}"), admin).andExpect(status().isBadRequest());
        perform(get("/v1/pacientes/9999999"), vet).andExpect(status().isNotFound());
    }

    @Test
    void patientAndTutorCanBeCreatedTogetherAndEditedWithoutDuplicateTutor() throws Exception {
        String data = """
                {"nome":"Mel","especie":"Canina","raca":"SRD","sexo":"Fêmea","peso":8.5,"dataNascimento":"2022-01-01",
                 "novoTutor":{"nome":"Beatriz","cpf":"52998224725","telefone":"11999999999","email":"bia@qrvet.test","endereco":"Rua B"}}
                """;
        perform(post("/v1/pacientes").content(data.replace("8.5", "-1")), receptionist).andExpect(status().isBadRequest());
        assertTrue(tutors.findByEmailIgnoreCase("bia@qrvet.test").isEmpty());
        JsonNode patient = body(perform(post("/v1/pacientes").content(data), receptionist).andExpect(status().isCreated()));
        long tutorId = patient.get("tutorId").asLong();
        perform(post("/v1/pacientes").content(data), receptionist).andExpect(status().isConflict());
        String update = """
                {"nome":"Mel atualizada","especie":"Canina","raca":"SRD","sexo":"Fêmea","peso":9.2,"dataNascimento":"2022-01-01","tutorId":%d}
                """.formatted(tutorId);
        perform(put("/v1/pacientes/" + patient.get("id").asLong()).content(update), vet).andExpect(status().isOk())
                .andExpect(jsonPath("$.peso").value(9.2)).andExpect(jsonPath("$.nome").value("Mel atualizada"));
        perform(put("/v1/tutores/" + tutorId).content("""
                {"nome":"Beatriz Silva","cpf":"52998224725","telefone":"11888888888","email":"bia@qrvet.test","endereco":"Rua C"}
                """), receptionist).andExpect(status().isOk()).andExpect(jsonPath("$.telefone").value("11888888888"));
        perform(post("/v1/pacientes").content(data.replace("\"novoTutor\"", "\"tutorId\":" + tutorId + ",\"novoTutor\"")), vet)
                .andExpect(status().isBadRequest());
    }

    private JsonNode schedule(long admissionId, Instant at) throws Exception {
        return body(perform(post("/v1/internacoes/" + admissionId + "/alimentacao/agenda").content("""
                {"alimento":"Ração úmida","quantidade":"40 g","horarios":["%s"]}
                """.formatted(at)), assistant).andExpect(status().isCreated())).get(0);
    }

    @Test
    void feedingChecklistRespectsFastingAndCompletesExactlyOnceWithAuditTrail() throws Exception {
        JsonNode admission = openHospitalization();
        long id = admission.get("id").asLong();
        Instant time = Instant.now().plusSeconds(3600);
        JsonNode item = schedule(id, time);
        String path = "/v1/internacoes/" + id + "/alimentacao/agenda/" + item.get("id").asLong();
        perform(put(path + "/concluir").content("{}"), assistant).andExpect(status().isBadRequest());
        perform(get("/v1/alimentacao/agenda").param("inicio", time.minusSeconds(1).toString()).param("fim", time.plusSeconds(1).toString()), assistant)
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].pacienteNome").value("Luna"));
        // Make this meal due without sleeping; the API still rejects past times on creation.
        jdbc.update("update alimentacao_agendada set horario = ? where id = ?", Timestamp.from(Instant.now().minusSeconds(60)), item.get("id").asLong());
        entities.clear();
        perform(post("/v1/internacoes/" + id + "/jejum").content("{\"motivo\":\"Exame\"}"), vet).andExpect(status().isCreated());
        perform(put(path + "/concluir").content("{}"), assistant).andExpect(status().isBadRequest());
        perform(put("/v1/internacoes/" + id + "/jejum/encerrar"), vet).andExpect(status().isOk());
        perform(put(path + "/concluir").content("{\"observacao\":\"Boa aceitação\"}"), assistant).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA")).andExpect(jsonPath("$.responsavelNome").value(assistant.getName()))
                .andExpect(jsonPath("$.concluidaEm").isNotEmpty());
        perform(put(path + "/concluir").content("{}"), vet).andExpect(status().isOk());
        perform(get("/v1/internacoes/" + id + "/alimentacao"), vet).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].aceitacaoObservacao").value("Boa aceitação"));
        perform(put(path + "/cancelar").content("{\"motivo\":\"Erro\"}"), vet).andExpect(status().isConflict());
        perform(get("/v1/alimentacao/agenda").param("inicio", time.minusSeconds(7200).toString()).param("fim", time.toString()), receptionist)
                .andExpect(status().isForbidden());
    }

    @Test
    void schedulesRejectDuplicatesAndClosedAdmissionsCancelPendingMeals() throws Exception {
        long id = openHospitalization().get("id").asLong();
        Instant time = Instant.now().plusSeconds(3600);
        JsonNode item = schedule(id, time);
        String create = "/v1/internacoes/" + id + "/alimentacao/agenda";
        perform(post(create).content("""
                {"alimento":"Ração","quantidade":"40 g","horarios":["%s","%s"]}
                """.formatted(time.plusSeconds(3600), time)), assistant).andExpect(status().isConflict());
        perform(post(create).content("""
                {"alimento":"Ração","quantidade":"40 g","horarios":["%s"]}
                """.formatted(Instant.now().minusSeconds(10))), assistant).andExpect(status().isBadRequest());
        perform(get("/v1/alimentacao/agenda").param("inicio", time.minusSeconds(1).toString()).param("fim", time.plusSeconds(7200).toString()), vet)
                .andExpect(jsonPath("$.length()").value(1));
        JsonNode other = schedule(id, time.plusSeconds(3600));
        String otherPath = create + "/" + other.get("id").asLong();
        perform(put(otherPath + "/cancelar").content("{\"motivo\":\"Mudança na dieta\"}"), vet).andExpect(status().isOk())
                .andExpect(jsonPath("$.motivoCancelamento").value("Mudança na dieta"));
        perform(put(otherPath + "/concluir").content("{}"), assistant).andExpect(status().isConflict());
        // A corrected plan may reuse a cancelled time, keeping the cancelled entry as history.
        schedule(id, time.plusSeconds(3600));
        perform(put("/v1/internacoes/" + id + "/encerrar").content("{\"statusEncerramento\":\"ALTA\"}"), receptionist).andExpect(status().isOk());
        perform(get("/v1/alimentacao/agenda").param("inicio", time.minusSeconds(1).toString()).param("fim", time.plusSeconds(1).toString()), vet)
                .andExpect(jsonPath("$[0].status").value("CANCELADA")).andExpect(jsonPath("$[0].motivoCancelamento").value("Internação encerrada"));
        perform(put(create + "/" + item.get("id").asLong() + "/concluir").content("{}"), assistant).andExpect(status().isConflict());
    }

    @Test
    void localPhotoUploadValidatesBytesAndRequiresPrivatePatientAccess() throws Exception {
        long id = openHospitalization().get("pacienteId").asLong();
        String path = "/v1/pacientes/" + id + "/foto";
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(24, 24, BufferedImage.TYPE_INT_RGB), "png", output);
        MockMultipartFile photo = new MockMultipartFile("foto", "../../foto.png", "image/png", output.toByteArray());
        mvc.perform(multipart(path).file(photo).header("Authorization", "Bearer " + tokens.generateAccessToken(receptionist, Instant.now()))
                .cookie(new Cookie("XSRF-TOKEN", "test-csrf")).header("X-XSRF-TOKEN", "test-csrf"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fotoVersao").isNotEmpty());
        perform(get(path), receptionist).andExpect(status().isOk()).andExpect(content().contentType("image/jpeg"));
        perform(get(path), assistant).andExpect(status().isForbidden());
        perform(get(path), null).andExpect(status().isUnauthorized());
        mvc.perform(multipart(path).file(new MockMultipartFile("foto", "fake.png", "image/png", "<svg>fake</svg>".getBytes()))
                .header("Authorization", "Bearer " + tokens.generateAccessToken(vet, Instant.now()))
                .cookie(new Cookie("XSRF-TOKEN", "test-csrf")).header("X-XSRF-TOKEN", "test-csrf"))
                .andExpect(status().isBadRequest());
        perform(delete(path), receptionist).andExpect(status().isNoContent());
        perform(get(path), vet).andExpect(status().isNotFound());
        perform(get("/v1/pacientes/" + id), vet).andExpect(jsonPath("$.fotoVersao").isEmpty());
    }
}
