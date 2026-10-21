package ipiranga.fatec.qrvet.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ipiranga.fatec.qrvet.dtos.request.AgendaAlimentacaoRequest;
import ipiranga.fatec.qrvet.dtos.request.JejumRequest;
import ipiranga.fatec.qrvet.dtos.request.RegistroAlimentacaoRequest;
import ipiranga.fatec.qrvet.dtos.response.AgendaAlimentacaoResponse;
import ipiranga.fatec.qrvet.dtos.response.JejumResponse;
import ipiranga.fatec.qrvet.dtos.response.RegistroAlimentacaoResponse;
import ipiranga.fatec.qrvet.exceptions.InvalidRequestException;
import ipiranga.fatec.qrvet.exceptions.OperationConflictException;
import ipiranga.fatec.qrvet.exceptions.ResourceNotFoundException;
import ipiranga.fatec.qrvet.models.AlimentacaoAgendada;
import ipiranga.fatec.qrvet.models.Internacao;
import ipiranga.fatec.qrvet.models.Jejum;
import ipiranga.fatec.qrvet.models.RegistroAlimentacao;
import ipiranga.fatec.qrvet.models.User;
import ipiranga.fatec.qrvet.models.enums.InternacaoStatus;
import ipiranga.fatec.qrvet.repositories.AlimentacaoAgendadaRepository;
import ipiranga.fatec.qrvet.repositories.InternacaoRepository;
import ipiranga.fatec.qrvet.repositories.JejumRepository;
import ipiranga.fatec.qrvet.repositories.RegistroAlimentacaoRepository;
import ipiranga.fatec.qrvet.security.CurrentUser;
import ipiranga.fatec.qrvet.specifications.RegistroAlimentacaoSpecifications;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class NutricaoClinicaService {
    private static final Logger logger = LoggerFactory.getLogger(NutricaoClinicaService.class);

    private final AlimentacaoAgendadaRepository agendaRepository;
    private final RegistroAlimentacaoRepository registroRepository;
    private final JejumRepository jejumRepository;
    private final InternacaoRepository internacaoRepository;
    private final CurrentUser currentUser;

    public NutricaoClinicaService(
            AlimentacaoAgendadaRepository agendaRepository,
            RegistroAlimentacaoRepository registroRepository,
            JejumRepository jejumRepository,
            InternacaoRepository internacaoRepository,
            CurrentUser currentUser) {
        this.agendaRepository = agendaRepository;
        this.registroRepository = registroRepository;
        this.jejumRepository = jejumRepository;
        this.internacaoRepository = internacaoRepository;
        this.currentUser = currentUser;
    }

    // --- ALIMENTAÇÃO AGENDADA ---

    @Transactional
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO')")
    public List<AgendaAlimentacaoResponse> createSchedule(Long internacaoId, AgendaAlimentacaoRequest request) {
        logger.info("Scheduling feeding routine for admission {}", internacaoId);
        Internacao admission = lockAdmission(internacaoId);
        if (admission.getStatus() != InternacaoStatus.ATIVA) {
            throw new InvalidRequestException("Só é possível agendar em uma internação ativa.");
        }

        List<Instant> times = request.horarios().stream()
                .map(t -> t.truncatedTo(ChronoUnit.SECONDS))
                .sorted()
                .toList();

        if (new HashSet<>(times).size() != times.size()) {
            throw new InvalidRequestException("Há horários repetidos no agendamento.");
        }
        if (Duration.between(times.getFirst(), times.getLast()).compareTo(Duration.ofDays(31)) > 0) {
            throw new InvalidRequestException("Agende no máximo 31 dias por vez.");
        }

        for (Instant time : times) {
            if (!time.isAfter(Instant.now())) {
                throw new InvalidRequestException("Selecione horários futuros.");
            }
            if (agendaRepository.existsByInternacaoIdAndHorarioAndStatusNot(internacaoId, time, AlimentacaoAgendada.Status.CANCELADA)) {
                throw new OperationConflictException("Já existe alimentação agendada nesse horário. Confira a agenda do paciente.");
            }
        }

        User user = currentUser.getCurrent();
        boolean fasting = jejumRepository.existsByInternacaoIdAndAtivoTrue(internacaoId);

        List<AlimentacaoAgendada> items = times.stream()
                .map(time -> new AlimentacaoAgendada(admission, request.alimento().trim(), request.quantidade().trim(), time, user))
                .toList();

        return agendaRepository.saveAllAndFlush(items).stream()
                .map(a -> AgendaAlimentacaoResponse.from(a, fasting))
                .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO')")
    public List<AgendaAlimentacaoResponse> checklist(Instant inicio, Instant fim, Long internacaoId) {
        if (!fim.isAfter(inicio) || Duration.between(inicio, fim).compareTo(Duration.ofDays(32)) > 0) {
            throw new InvalidRequestException("Selecione um período de até 31 dias.");
        }
        Set<Long> fastingAdmissionIds = new HashSet<>(jejumRepository.activeAdmissionIds());
        return agendaRepository.checklist(inicio, fim, internacaoId).stream()
                .map(a -> AgendaAlimentacaoResponse.from(a, fastingAdmissionIds.contains(a.getInternacao().getId())))
                .toList();
    }

    @Transactional
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO')")
    public AgendaAlimentacaoResponse completeSchedule(Long internacaoId, Long agendaId, String observacao) {
        logger.info("Completing scheduled feeding {} for admission {}", agendaId, internacaoId);
        Internacao admission = lockAdmission(internacaoId);
        AlimentacaoAgendada item = findSchedule(agendaId, internacaoId);

        boolean fasting = jejumRepository.existsByInternacaoIdAndAtivoTrue(internacaoId);
        if (item.getStatus() == AlimentacaoAgendada.Status.CONCLUIDA) {
            return AgendaAlimentacaoResponse.from(item, fasting);
        }
        if (item.getStatus() != AlimentacaoAgendada.Status.PENDENTE) {
            throw new OperationConflictException("Este agendamento foi cancelado.");
        }
        if (admission.getStatus() != InternacaoStatus.ATIVA) {
            throw new InvalidRequestException("A internação foi encerrada.");
        }
        if (item.getHorario().isAfter(Instant.now())) {
            throw new InvalidRequestException("Aguarde o horário agendado para concluir.");
        }
        if (fasting) {
            throw new InvalidRequestException("Food cannot be recorded while the hospitalization has active fasting.");
        }

        String obs = (observacao != null && !observacao.isBlank()) ? observacao.trim() : null;
        RegistroAlimentacao registro = RegistroAlimentacao.builder()
                .internacao(admission)
                .usuario(currentUser.getCurrent())
                .alimento(item.getAlimento())
                .quantidade(item.getQuantidade())
                .aceitacaoObservacao(obs)
                .build();
        registro = registroRepository.saveAndFlush(registro);

        item.complete(registro);
        return AgendaAlimentacaoResponse.from(item, false);
    }

    @Transactional
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO')")
    public AgendaAlimentacaoResponse cancelSchedule(Long internacaoId, Long agendaId, String reason) {
        logger.info("Canceling scheduled feeding {} for admission {}", agendaId, internacaoId);
        lockAdmission(internacaoId);
        AlimentacaoAgendada item = findSchedule(agendaId, internacaoId);
        if (item.getStatus() != AlimentacaoAgendada.Status.PENDENTE) {
            throw new OperationConflictException("Somente agendamentos pendentes podem ser cancelados.");
        }
        item.cancel(reason.trim());
        return AgendaAlimentacaoResponse.from(item, jejumRepository.existsByInternacaoIdAndAtivoTrue(internacaoId));
    }

    // --- JEJUM ---

    @Transactional
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO')")
    public JejumResponse startFasting(Long internacaoId, JejumRequest request) {
        logger.info("Starting fasting for admission {}", internacaoId);
        Internacao internacao = lockAdmission(internacaoId);

        if (internacao.getStatus() != InternacaoStatus.ATIVA) {
            throw new InvalidRequestException("Fasting can only be changed for an active hospitalization.");
        }
        if (jejumRepository.existsByInternacaoIdAndAtivoTrue(internacaoId)) {
            throw new OperationConflictException("The hospitalization already has an active fasting period.");
        }

        Jejum jejum = Jejum.builder()
                .internacao(internacao)
                .motivo(request.motivo().trim())
                .build();
        return JejumResponse.from(jejumRepository.saveAndFlush(jejum));
    }

    @Transactional
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO')")
    public JejumResponse endFasting(Long internacaoId) {
        logger.info("Ending fasting for admission {}", internacaoId);
        Internacao internacao = lockAdmission(internacaoId);
        if (internacao.getStatus() != InternacaoStatus.ATIVA) {
            throw new InvalidRequestException("Fasting can only be changed for an active hospitalization.");
        }
        Jejum jejum = jejumRepository.findActiveForUpdate(internacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Active fasting period not found."));
        jejum.end();
        return JejumResponse.from(jejum);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO')")
    public List<JejumResponse> fastingHistory(Long internacaoId) {
        Internacao internacao = internacaoRepository.findById(internacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospitalization not found."));
        return jejumRepository.findAllByInternacaoIdOrderByDataHoraInicioDesc(internacao.getId())
                .stream()
                .map(JejumResponse::from)
                .toList();
    }

    // --- REGISTRO DE ALIMENTAÇÃO DIRETO ---

    @Transactional
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO')")
    public RegistroAlimentacaoResponse registerFood(Long internacaoId, RegistroAlimentacaoRequest request) {
        logger.info("Recording food intake for admission {}", internacaoId);
        Internacao internacao = lockAdmission(internacaoId);
        if (internacao.getStatus() != InternacaoStatus.ATIVA) {
            throw new InvalidRequestException("Food can only be recorded for an active hospitalization.");
        }
        if (jejumRepository.existsByInternacaoIdAndAtivoTrue(internacaoId)) {
            throw new InvalidRequestException("Food cannot be recorded while the hospitalization has active fasting.");
        }

        String observacao = null;
        if (request.aceitacaoObservacao() != null && !request.aceitacaoObservacao().isBlank()) {
            observacao = request.aceitacaoObservacao().trim();
        }

        RegistroAlimentacao registro = RegistroAlimentacao.builder()
                .internacao(internacao)
                .usuario(currentUser.getCurrent())
                .alimento(request.alimento().trim())
                .quantidade(request.quantidade().trim())
                .aceitacaoObservacao(observacao)
                .build();
        return RegistroAlimentacaoResponse.from(registroRepository.saveAndFlush(registro));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO')")
    public List<RegistroAlimentacaoResponse> foodHistory(Long internacaoId) {
        if (!internacaoRepository.existsById(internacaoId)) {
            throw new ResourceNotFoundException("Hospitalization not found.");
        }
        return registroRepository.findAll(
                        RegistroAlimentacaoSpecifications.byInternacaoId(internacaoId),
                        Sort.by(Sort.Direction.DESC, "dataHoraRegistro"))
                .stream()
                .map(RegistroAlimentacaoResponse::from)
                .toList();
    }

    // --- CICLO COM A INTERNAÇÃO ---

    @Transactional
    public void cancelForDischarge(Long internacaoId) {
        logger.info("Discharge: canceling pending feeding schedules and ending active fasting for admission {}", internacaoId);
        agendaRepository.findAllByInternacaoIdAndStatus(internacaoId, AlimentacaoAgendada.Status.PENDENTE)
                .forEach(a -> a.cancel("Internação encerrada"));
        jejumRepository.findActiveForUpdate(internacaoId).ifPresent(Jejum::end);
    }

    @Transactional(readOnly = true)
    public boolean isFastingActive(Long internacaoId) {
        return jejumRepository.existsByInternacaoIdAndAtivoTrue(internacaoId);
    }

    // --- MÉTODOS PRIVADOS DE SUPORTE ---

    private Internacao lockAdmission(Long id) {
        return internacaoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospitalization not found."));
    }

    private AlimentacaoAgendada findSchedule(Long id, Long admissionId) {
        return agendaRepository.findById(id)
                .filter(a -> a.getInternacao().getId().equals(admissionId))
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado."));
    }
}
