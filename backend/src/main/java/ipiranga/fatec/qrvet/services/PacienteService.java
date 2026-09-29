package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.request.PacienteRequest;
import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.dtos.response.PacienteResponse;
import ipiranga.fatec.qrvet.dtos.response.PageResponse;
import ipiranga.fatec.qrvet.exceptions.ResourceNotFoundException;
import ipiranga.fatec.qrvet.models.Paciente;
import ipiranga.fatec.qrvet.repositories.PacienteRepository;
import ipiranga.fatec.qrvet.repositories.TutorRepository;
import ipiranga.fatec.qrvet.utils.PaginationUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
public class PacienteService {
    private final PacienteRepository pacienteRepository;
    private final TutorRepository tutorRepository;
    private final TutorService tutorService;

    public PacienteService(PacienteRepository pacienteRepository, TutorRepository tutorRepository, TutorService tutorService) {
        this.pacienteRepository = pacienteRepository;
        this.tutorRepository = tutorRepository;
        this.tutorService = tutorService;
    }

    @Transactional
    public PacienteResponse create(PacienteRequest request) {
        Long tutorId = request.novoTutor() == null ? request.tutorId() : tutorService.create(request.novoTutor()).id();
        var tutor = tutorRepository.findById(tutorId)
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
        return PacienteResponse.from(pacienteRepository.saveAndFlush(paciente));
    }

    @Transactional(readOnly = true)
    public PacienteResponse findById(Long id) {
        return PacienteResponse.from(pacienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found.")));
    }

    @Transactional
    public PacienteResponse update(Long id, PacienteRequest request) {
        var paciente = pacienteRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found."));
        Long tutorId = request.novoTutor() == null ? request.tutorId() : tutorService.create(request.novoTutor()).id();
        paciente.setTutor(tutorRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tutor not found.")));
        paciente.setNome(request.nome().trim());
        paciente.setEspecie(request.especie().trim());
        paciente.setRaca(request.raca().trim());
        paciente.setSexo(request.sexo().trim());
        paciente.setDataNascimento(request.dataNascimento());
        paciente.setPeso(request.peso());
        paciente.setObservacoes(request.observacoes());
        return PacienteResponse.from(pacienteRepository.saveAndFlush(paciente));
    }

    @Transactional(readOnly = true)
    public PageResponse<PacienteResponse> list(PaginationRequest pagination) {
        return PageResponse.from(pacienteRepository.findAll(PaginationUtils.byId(pagination))
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
