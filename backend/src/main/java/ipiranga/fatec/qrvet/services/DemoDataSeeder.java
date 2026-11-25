package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.models.AlimentacaoAgendada;
import ipiranga.fatec.qrvet.models.Baia;
import ipiranga.fatec.qrvet.models.Evolucao;
import ipiranga.fatec.qrvet.models.Internacao;
import ipiranga.fatec.qrvet.models.Jejum;
import ipiranga.fatec.qrvet.models.Paciente;
import ipiranga.fatec.qrvet.models.RegistroAlimentacao;
import ipiranga.fatec.qrvet.models.RegistroMedicacao;
import ipiranga.fatec.qrvet.models.Tutor;
import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.models.enums.AuditAction;
import ipiranga.fatec.qrvet.models.enums.AuditResource;
import ipiranga.fatec.qrvet.models.enums.BaiaStatus;
import ipiranga.fatec.qrvet.models.enums.InternacaoStatus;
import ipiranga.fatec.qrvet.models.enums.Role;
import ipiranga.fatec.qrvet.repositories.AlimentacaoAgendadaRepository;
import ipiranga.fatec.qrvet.repositories.BaiaRepository;
import ipiranga.fatec.qrvet.repositories.EvolucaoRepository;
import ipiranga.fatec.qrvet.repositories.InternacaoRepository;
import ipiranga.fatec.qrvet.repositories.JejumRepository;
import ipiranga.fatec.qrvet.repositories.PacienteRepository;
import ipiranga.fatec.qrvet.repositories.RegistroAlimentacaoRepository;
import ipiranga.fatec.qrvet.repositories.RegistroMedicacaoRepository;
import ipiranga.fatec.qrvet.repositories.TutorRepository;
import ipiranga.fatec.qrvet.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
public class DemoDataSeeder {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;
    private final PacienteRepository pacienteRepository;
    private final BaiaRepository baiaRepository;
    private final InternacaoRepository internacaoRepository;
    private final EvolucaoRepository evolucaoRepository;
    private final JejumRepository jejumRepository;
    private final RegistroAlimentacaoRepository registroAlimentacaoRepository;
    private final AlimentacaoAgendadaRepository alimentacaoAgendadaRepository;
    private final RegistroMedicacaoRepository registroMedicacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public DemoDataSeeder(
            UserRepository userRepository,
            TutorRepository tutorRepository,
            PacienteRepository pacienteRepository,
            BaiaRepository baiaRepository,
            InternacaoRepository internacaoRepository,
            EvolucaoRepository evolucaoRepository,
            JejumRepository jejumRepository,
            RegistroAlimentacaoRepository registroAlimentacaoRepository,
            AlimentacaoAgendadaRepository alimentacaoAgendadaRepository,
            RegistroMedicacaoRepository registroMedicacaoRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.tutorRepository = tutorRepository;
        this.pacienteRepository = pacienteRepository;
        this.baiaRepository = baiaRepository;
        this.internacaoRepository = internacaoRepository;
        this.evolucaoRepository = evolucaoRepository;
        this.jejumRepository = jejumRepository;
        this.registroAlimentacaoRepository = registroAlimentacaoRepository;
        this.alimentacaoAgendadaRepository = alimentacaoAgendadaRepository;
        this.registroMedicacaoRepository = registroMedicacaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public void seed(String rawPassword) {
        String effectivePassword = (rawPassword == null || rawPassword.isBlank()) ? "teste@123" : rawPassword;

        ensureUser("Administrador QRVet", "admin@qrvet.com", Role.ADMIN, effectivePassword);
        User veterinario = ensureUser("Dra. Helena Prado", "veterinario@qrvet.com", Role.VETERINARIO, effectivePassword);
        ensureUser("Lucas Andrade", "recepcao@qrvet.com", Role.RECEPCIONISTA, effectivePassword);
        User auxiliar = ensureUser("Camila Souza", "auxiliar@qrvet.com", Role.AUXILIAR_TECNICO, effectivePassword);

        Baia baia1 = ensureBaia("B-01", BaiaStatus.DISPONIVEL, "Baia para cães de médio e grande porte");
        Baia baia2 = ensureBaia("B-02", BaiaStatus.DISPONIVEL, "Baia felina climatizada e silenciosa");
        Baia baia3 = ensureBaia("B-03", BaiaStatus.DISPONIVEL, "Baia para recuperação pós-operatória");
        ensureBaia("B-04", BaiaStatus.MANUTENCAO, "Limpeza e desinfecção terminal");

        if (internacaoRepository.count() > 0) {
            log.info("Demo hospitalizations already exist; skipping clinical seed data");
            return;
        }

        Tutor carlos = ensureTutor(
                "Carlos Eduardo Mendes",
                "39053344705",
                "(11) 98765-4321",
                "carlos.mendes@exemplo.com",
                "Rua Vergueiro, 2350 - Vila Mariana, São Paulo/SP");
        Tutor mariana = ensureTutor(
                "Mariana Costa",
                "52998224725",
                "(11) 97654-3210",
                "mariana.costa@exemplo.com",
                "Av. Nazaré, 1120 - Ipiranga, São Paulo/SP");
        Tutor patricia = ensureTutor(
                "Patrícia Almeida",
                "11144477735",
                "(11) 96543-2109",
                "patricia.almeida@exemplo.com",
                "Rua Bom Pastor, 840 - Ipiranga, São Paulo/SP");

        Paciente thor = createPatient(
                carlos,
                "Thor",
                "Canino",
                "Golden Retriever",
                "Macho",
                LocalDate.now().minusYears(4),
                new BigDecimal("28.50"),
                "Paciente dócil, histórico de sensibilidade gástrica.",
                "Alergia a dipirona",
                "Dieta pastosa gastrointestinal fracionada",
                "Monitorar hidratação e mucosas a cada 4h");

        Paciente luna = createPatient(
                mariana,
                "Luna",
                "Felino",
                "Siamês",
                "Fêmea",
                LocalDate.now().minusYears(2),
                new BigDecimal("3.80"),
                "Assustada com barulhos altos; prefere caixa coberta.",
                null,
                "Ração úmida renal em temperatura morna",
                "Ambiente silencioso e manejo sem contenção brusca");

        Paciente bento = createPatient(
                patricia,
                "Bento",
                "Canino",
                "Beagle",
                "Macho",
                LocalDate.now().minusYears(5),
                new BigDecimal("12.40"),
                "Acompanhamento pós-cirúrgico concluído sem intercorrências.",
                null,
                "Ração seca habitual umedecida",
                "Uso contínuo de colar elizabetano");

        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        // 1. Internação Ativa: Thor na Baia B-01
        if (baia1.getStatus() == BaiaStatus.DISPONIVEL) {
            baia1.occupyAutomatically();
            baiaRepository.saveAndFlush(baia1);
        }
        Internacao internacaoThor = Internacao.builder()
                .paciente(thor)
                .baia(baia1)
                .veterinario(veterinario)
                .motivo("Vômitos recorrentes e apatia há 24 horas")
                .diagnosticoInicial("Gastroenterite aguda com desidratação leve")
                .observacoes("Fluidoterapia intravenosa contínua e controle antiemético.")
                .build();
        internacaoThor.setEntradaInternacao(now.minus(18, ChronoUnit.HOURS));
        internacaoThor = internacaoRepository.saveAndFlush(internacaoThor);

        evolucaoRepository.saveAndFlush(new Evolucao(
                internacaoThor,
                veterinario,
                "Admissão clínica: paciente alerta, mucosas normocoradas, TPC 2s, iniciada fluidoterapia com Ringer Lactato e antiemético IV."));
        evolucaoRepository.saveAndFlush(new Evolucao(
                internacaoThor,
                auxiliar,
                "Plantão noturno: sem novos episódios de êmese, aceitou primeira porção de dieta pastosa com bom apetite."));

        registroMedicacaoRepository.saveAndFlush(RegistroMedicacao.builder()
                .internacao(internacaoThor)
                .usuario(auxiliar)
                .medicamento("Ondansetrona")
                .dose("4")
                .unidade("mg")
                .via("Intravenosa (IV)")
                .prescricao("0,15 mg/kg a cada 8 horas")
                .observacoes("Aplicação lenta pelo acesso venoso cefálico, sem reação local.")
                .build());

        RegistroAlimentacao refeicaoConcluidaThor = registroAlimentacaoRepository.saveAndFlush(
                RegistroAlimentacao.builder()
                        .internacao(internacaoThor)
                        .usuario(auxiliar)
                        .alimento("Ração úmida gastrointestinal")
                        .quantidade("150 g")
                        .aceitacaoObservacao("Boa aceitação, consumiu toda a porção oferecida.")
                        .build());

        AlimentacaoAgendada agendaConcluida = new AlimentacaoAgendada(
                internacaoThor,
                "Ração úmida gastrointestinal",
                "150 g",
                now.minus(3, ChronoUnit.HOURS),
                veterinario);
        agendaConcluida.complete(refeicaoConcluidaThor);
        alimentacaoAgendadaRepository.saveAndFlush(agendaConcluida);

        AlimentacaoAgendada agendaPendenteAgora = new AlimentacaoAgendada(
                internacaoThor,
                "Ração úmida gastrointestinal",
                "150 g",
                now.minus(20, ChronoUnit.MINUTES),
                veterinario);
        alimentacaoAgendadaRepository.saveAndFlush(agendaPendenteAgora);

        AlimentacaoAgendada agendaFutura = new AlimentacaoAgendada(
                internacaoThor,
                "Ração úmida gastrointestinal",
                "150 g",
                now.plus(4, ChronoUnit.HOURS),
                veterinario);
        alimentacaoAgendadaRepository.saveAndFlush(agendaFutura);

        // 2. Internação Ativa: Luna na Baia B-02 (com Jejum Ativo)
        if (baia2.getStatus() == BaiaStatus.DISPONIVEL) {
            baia2.occupyAutomatically();
            baiaRepository.saveAndFlush(baia2);
        }
        Internacao internacaoLuna = Internacao.builder()
                .paciente(luna)
                .baia(baia2)
                .veterinario(veterinario)
                .motivo("Hiporexia e prostração para investigação renal")
                .diagnosticoInicial("Suspeita de doença renal crônica agudizada")
                .observacoes("Manter em jejum para coleta de exames bioquímicos e ultrassonografia abdominal.")
                .build();
        internacaoLuna.setEntradaInternacao(now.minus(6, ChronoUnit.HOURS));
        internacaoLuna = internacaoRepository.saveAndFlush(internacaoLuna);

        evolucaoRepository.saveAndFlush(new Evolucao(
                internacaoLuna,
                veterinario,
                "Paciente acomodada em baia felina silenciosa. Iniciado jejum alimentar para coleta de perfil renal e ultrassonografia."));

        jejumRepository.saveAndFlush(Jejum.builder()
                .internacao(internacaoLuna)
                .motivo("Coleta de exames bioquímicos e ultrassonografia abdominal")
                .build());

        // 3. Internação Encerrada (ALTA): Bento na Baia B-03 (baia permanece DISPONIVEL)
        Internacao internacaoBento = Internacao.builder()
                .paciente(bento)
                .baia(baia3)
                .veterinario(veterinario)
                .motivo("Observação pós-operatória de nodulectomia cutânea")
                .diagnosticoInicial("Pós-operatório imediato sem complicações")
                .observacoes("Alta concedida com receita domiciliar e retorno agendado para retirada de pontos.")
                .build();
        internacaoBento.setEntradaInternacao(now.minus(30, ChronoUnit.HOURS));
        internacaoBento = internacaoRepository.saveAndFlush(internacaoBento);
        internacaoBento.close(InternacaoStatus.ALTA);
        internacaoBento.setSaidaInternacao(now.minus(6, ChronoUnit.HOURS));
        internacaoBento = internacaoRepository.saveAndFlush(internacaoBento);

        evolucaoRepository.saveAndFlush(new Evolucao(
                internacaoBento,
                veterinario,
                "Paciente recuperado da anestesia, parâmetros vitais estáveis, ferida cirúrgica limpa e seca. Liberado para alta médica."));
        registroMedicacaoRepository.saveAndFlush(RegistroMedicacao.builder()
                .internacao(internacaoBento)
                .usuario(veterinario)
                .medicamento("Meloxicam")
                .dose("1,2")
                .unidade("mg")
                .via("Subcutânea (SC)")
                .prescricao("0,1 mg/kg a cada 24 horas")
                .observacoes("Dose anti-inflamatória pré-alta.")
                .build());
        registroAlimentacaoRepository.saveAndFlush(RegistroAlimentacao.builder()
                .internacao(internacaoBento)
                .usuario(auxiliar)
                .alimento("Ração seca umedecida")
                .quantidade("120 g")
                .aceitacaoObservacao("Alimentou-se espontaneamente antes da alta.")
                .build());

        auditService.log(
                AuditAction.HOSPITALIZATION_CREATED,
                AuditResource.HOSPITALIZATION,
                String.valueOf(internacaoThor.getId()),
                Map.of("patientId", thor.getId(), "bayId", baia1.getId(), "veterinarioNome", veterinario.getName()));
        auditService.log(
                AuditAction.HOSPITALIZATION_CREATED,
                AuditResource.HOSPITALIZATION,
                String.valueOf(internacaoLuna.getId()),
                Map.of("patientId", luna.getId(), "bayId", baia2.getId(), "veterinarioNome", veterinario.getName()));

        log.info("Demo seed data created successfully (4 test users, 4 bays, 3 tutors, 3 patients, 3 hospitalizations)");
    }

    private User ensureUser(String name, String email, Role role, String rawPassword) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> {
                    User created = User.builder()
                            .name(name)
                            .email(email)
                            .role(role)
                            .active(true)
                            .confirmed(true)
                            .passwordHash(passwordEncoder.encode(rawPassword))
                            .build();
                    log.info("Created demo user {} ({})", email, role);
                    return userRepository.saveAndFlush(created);
                });
    }

    private Baia ensureBaia(String identificacao, BaiaStatus status, String observacao) {
        return baiaRepository.findByIdentificacaoIgnoreCase(identificacao)
                .orElseGet(() -> baiaRepository.saveAndFlush(
                        Baia.builder()
                                .identificacao(identificacao)
                                .status(status)
                                .observacao(observacao)
                                .build()));
    }

    private Tutor ensureTutor(String nome, String cpf, String telefone, String email, String endereco) {
        return tutorRepository.findByCpf(cpf)
                .orElseGet(() -> tutorRepository.saveAndFlush(
                        Tutor.builder()
                                .nome(nome)
                                .cpf(cpf)
                                .telefone(telefone)
                                .email(email)
                                .endereco(endereco)
                                .build()));
    }

    private Paciente createPatient(
            Tutor tutor,
            String nome,
            String especie,
            String raca,
            String sexo,
            LocalDate dataNascimento,
            BigDecimal peso,
            String observacoes,
            String alergias,
            String restricoesAlimentares,
            String cuidadosManejo) {
        Paciente paciente = Paciente.builder()
                .tutor(tutor)
                .nome(nome)
                .especie(especie)
                .raca(raca)
                .sexo(sexo)
                .dataNascimento(dataNascimento)
                .peso(peso)
                .observacoes(observacoes)
                .build();
        paciente.setAlergias(alergias);
        paciente.setRestricoesAlimentares(restricoesAlimentares);
        paciente.setCuidadosManejo(cuidadosManejo);
        return pacienteRepository.saveAndFlush(paciente);
    }
}
