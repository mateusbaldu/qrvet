package ipiranga.fatec.qrvet.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ipiranga.fatec.qrvet.dtos.request.PacienteRequest;
import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.dtos.filter.PacienteSearchFilter;
import ipiranga.fatec.qrvet.dtos.response.PacienteResponse;
import ipiranga.fatec.qrvet.dtos.response.PageResponse;
import ipiranga.fatec.qrvet.exceptions.ResourceNotFoundException;
import ipiranga.fatec.qrvet.models.Paciente;
import ipiranga.fatec.qrvet.models.Tutor;
import ipiranga.fatec.qrvet.models.enums.AuditAction;
import ipiranga.fatec.qrvet.models.enums.AuditResource;
import ipiranga.fatec.qrvet.repositories.PacienteRepository;
import ipiranga.fatec.qrvet.repositories.TutorRepository;
import ipiranga.fatec.qrvet.utils.PaginationUtils;
import ipiranga.fatec.qrvet.specifications.PacienteSpecifications;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
public class PacienteService {
    private static final Logger logger = LoggerFactory.getLogger(PacienteService.class);
    private final PacienteRepository pacienteRepository;
    private final TutorRepository tutorRepository;
    private final TutorService tutorService;
    private final AuditService auditService;

    public PacienteService(PacienteRepository pacienteRepository, TutorRepository tutorRepository, TutorService tutorService,
                           AuditService auditService) {
        this.pacienteRepository = pacienteRepository;
        this.tutorRepository = tutorRepository;
        this.tutorService = tutorService;
        this.auditService = auditService;
    }

    @Transactional
    public PacienteResponse create(PacienteRequest request) {
        logger.info("Creating patient record");
        Long tutorId;
        if (request.novoTutor() == null) {
            tutorId = request.tutorId();
        } else {
            tutorId = tutorService.create(request.novoTutor()).id();
        }
        Tutor tutor = tutorRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tutor not found."));
        Paciente paciente = Paciente.builder()
                .tutor(tutor)
                .nome(request.nome().trim())
                .especie(request.especie().trim())
                .raca(request.raca().trim())
                .sexo(request.sexo().trim())
                .dataNascimento(request.dataNascimento())
                .peso(request.peso())
                .observacoes(request.observacoes())
                .build();
        PacienteResponse response = PacienteResponse.from(pacienteRepository.saveAndFlush(paciente));
        logger.info("Patient {} created", response.id());
        auditService.log(AuditAction.PATIENT_CREATED, AuditResource.PATIENT, String.valueOf(response.id()),
                Map.of("name", response.nome(), "species", response.especie()));
        return response;
    }

    @Transactional(readOnly = true)
    public PacienteResponse findById(Long id) {
        return PacienteResponse.from(pacienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found.")));
    }

    @Transactional
    public PacienteResponse update(Long id, PacienteRequest request) {
        logger.info("Updating patient {}", id);
        Paciente paciente = pacienteRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found."));
        Long tutorId;
        if (request.novoTutor() == null) {
            tutorId = request.tutorId();
        } else {
            tutorId = tutorService.create(request.novoTutor()).id();
        }
        paciente.setTutor(tutorRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tutor not found.")));
        paciente.setNome(request.nome().trim());
        paciente.setEspecie(request.especie().trim());
        paciente.setRaca(request.raca().trim());
        paciente.setSexo(request.sexo().trim());
        paciente.setDataNascimento(request.dataNascimento());
        paciente.setPeso(request.peso());
        paciente.setObservacoes(request.observacoes());
        PacienteResponse response = PacienteResponse.from(pacienteRepository.saveAndFlush(paciente));
        logger.info("Patient {} updated", id);
        auditService.log(AuditAction.PATIENT_UPDATED, AuditResource.PATIENT, String.valueOf(id),
                Map.of("name", response.nome(), "species", response.especie()));
        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<PacienteResponse> list(PaginationRequest pagination, PacienteSearchFilter filter) {
        return PageResponse.from(pacienteRepository.findAll(PacienteSpecifications.matches(filter), PaginationUtils.byId(pagination))
                .map(PacienteResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<PacienteResponse> listByTutor(Long tutorId, PaginationRequest pagination) {
        if (!tutorRepository.existsById(tutorId))
            throw new ResourceNotFoundException("Tutor not found.");
        return PageResponse.from(pacienteRepository.findAllByTutorId(tutorId, PaginationUtils.byId(pagination))
                .map(PacienteResponse::from));
    }
}
