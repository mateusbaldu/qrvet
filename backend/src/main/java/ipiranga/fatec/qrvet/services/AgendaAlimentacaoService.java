package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.request.AgendaAlimentacaoRequest;
import ipiranga.fatec.qrvet.dtos.request.RegistroAlimentacaoRequest;
import ipiranga.fatec.qrvet.dtos.response.AgendaAlimentacaoResponse;
import ipiranga.fatec.qrvet.exceptions.*;
import ipiranga.fatec.qrvet.models.*;
import ipiranga.fatec.qrvet.models.enums.InternacaoStatus;
import ipiranga.fatec.qrvet.repositories.*;
import ipiranga.fatec.qrvet.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO')")
public class AgendaAlimentacaoService {
    private final AlimentacaoAgendadaRepository agenda;
    private final InternacaoRepository admissions;
    private final JejumRepository fasts;
    private final RegistroAlimentacaoRepository records;
    private final RegistroAlimentacaoService food;
    private final CurrentUser current;
    public AgendaAlimentacaoService(AlimentacaoAgendadaRepository agenda, InternacaoRepository admissions,
            JejumRepository fasts, RegistroAlimentacaoRepository records, RegistroAlimentacaoService food, CurrentUser current) {
        this.agenda = agenda; this.admissions = admissions; this.fasts = fasts;
        this.records = records; this.food = food; this.current = current;
    }

    @Transactional
    public List<AgendaAlimentacaoResponse> create(Long id, AgendaAlimentacaoRequest request) {
        var admission = lockAdmission(id);
        if (admission.getStatus() != InternacaoStatus.ATIVA) throw new InvalidRequestException("Só é possível agendar em uma internação ativa.");
        // Use second precision consistently with the checklist and database comparisons.
        var times = request.horarios().stream().map(t -> t.truncatedTo(java.time.temporal.ChronoUnit.SECONDS)).sorted().toList();
        if (new HashSet<>(times).size() != times.size()) throw new InvalidRequestException("Há horários repetidos no agendamento.");
        if (Duration.between(times.getFirst(), times.getLast()).compareTo(Duration.ofDays(31)) > 0)
            throw new InvalidRequestException("Agende no máximo 31 dias por vez.");
        for (Instant time : times) {
            if (!time.isAfter(Instant.now())) throw new InvalidRequestException("Selecione horários futuros.");
            // All schedule mutations share the hospitalization lock, including cancellation and discharge.
            if (agenda.existsByInternacaoIdAndHorarioAndStatusNot(id, time, AlimentacaoAgendada.Status.CANCELADA))
                throw new OperationConflictException("Já existe alimentação agendada nesse horário. Confira a agenda do paciente.");
        }
        var user = current.getCurrent();
        boolean fasting = fasts.existsByInternacaoIdAndAtivoTrue(id);
        return agenda.saveAllAndFlush(times.stream().map(time -> new AlimentacaoAgendada(admission,
                        request.alimento().trim(), request.quantidade().trim(), time, user)).toList())
                .stream().map(a -> AgendaAlimentacaoResponse.from(a, fasting)).toList();
    }

    @Transactional(readOnly = true)
    public List<AgendaAlimentacaoResponse> checklist(Instant inicio, Instant fim, Long internacaoId) {
        if (!fim.isAfter(inicio) || Duration.between(inicio, fim).compareTo(Duration.ofDays(32)) > 0)
            throw new InvalidRequestException("Selecione um período de até 31 dias.");
        Set<Long> fastingIds = new HashSet<>(fasts.activeAdmissionIds());
        return agenda.checklist(inicio, fim, internacaoId).stream()
                .map(a -> AgendaAlimentacaoResponse.from(a, fastingIds.contains(a.getInternacao().getId()))).toList();
    }

    @Transactional
    public AgendaAlimentacaoResponse complete(Long admissionId, Long id, String observacao) {
        var admission = lockAdmission(admissionId);
        var item = find(id, admissionId);
        boolean fasting = fasts.existsByInternacaoIdAndAtivoTrue(admissionId);
        if (item.getStatus() == AlimentacaoAgendada.Status.CONCLUIDA) return AgendaAlimentacaoResponse.from(item, fasting);
        if (item.getStatus() != AlimentacaoAgendada.Status.PENDENTE) throw new OperationConflictException("Este agendamento foi cancelado.");
        if (admission.getStatus() != InternacaoStatus.ATIVA) throw new InvalidRequestException("A internação foi encerrada.");
        if (item.getHorario().isAfter(Instant.now())) throw new InvalidRequestException("Aguarde o horário agendado para concluir.");
        var record = food.create(admissionId, new RegistroAlimentacaoRequest(item.getAlimento(), item.getQuantidade(), observacao));
        item.complete(records.getReferenceById(record.id()));
        return AgendaAlimentacaoResponse.from(item, false);
    }

    @Transactional
    public AgendaAlimentacaoResponse cancel(Long admissionId, Long id, String reason) {
        lockAdmission(admissionId);
        var item = find(id, admissionId);
        if (item.getStatus() != AlimentacaoAgendada.Status.PENDENTE) throw new OperationConflictException("Somente agendamentos pendentes podem ser cancelados.");
        item.cancel(reason.trim());
        return AgendaAlimentacaoResponse.from(item, fasts.existsByInternacaoIdAndAtivoTrue(admissionId));
    }

    private Internacao lockAdmission(Long id) {
        return admissions.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Hospitalization not found."));
    }
    private AlimentacaoAgendada find(Long id, Long admissionId) {
        return agenda.findById(id).filter(a -> a.getInternacao().getId().equals(admissionId))
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado."));
    }
}
