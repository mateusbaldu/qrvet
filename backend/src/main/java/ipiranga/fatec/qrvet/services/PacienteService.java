package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.request.PacienteRequest;
import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.dtos.response.PacienteResponse;
import ipiranga.fatec.qrvet.dtos.response.PageResponse;
import ipiranga.fatec.qrvet.exceptions.ResourceNotFoundException;
import ipiranga.fatec.qrvet.models.Paciente;
import ipiranga.fatec.qrvet.repositories.PacienteRepository;
import ipiranga.fatec.qrvet.repositories.TutorRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
public class PacienteService {
    private final PacienteRepository pacienteRepository;
    private final TutorRepository tutorRepository;

    public PacienteService(PacienteRepository pacienteRepository, TutorRepository tutorRepository) {
        this.pacienteRepository = pacienteRepository;
        this.tutorRepository = tutorRepository;
    }

    @Transactional
    public PacienteResponse create(PacienteRequest request) {
        var tutor = tutorRepository.findById(request.tutorId())
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
    public PageResponse<PacienteResponse> list(PaginationRequest pagination) {
        return PageResponse.from(pacienteRepository.findAll(pageable(pagination)).map(PacienteResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<PacienteResponse> listByTutor(Long tutorId, PaginationRequest pagination) {
        if (!tutorRepository.existsById(tutorId))
            throw new ResourceNotFoundException("Tutor not found.");
        return PageResponse.from(pacienteRepository.findAllByTutorId(tutorId, pageable(pagination))
                .map(PacienteResponse::from));
    }

    private PageRequest pageable(PaginationRequest pagination) {
        return PageRequest.of(pagination.page(), pagination.size(), Sort.by("id").descending());
    }
}
