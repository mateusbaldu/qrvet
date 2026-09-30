package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.filter.TutorSearchFilter;
import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.dtos.request.TutorRequest;
import ipiranga.fatec.qrvet.dtos.response.PageResponse;
import ipiranga.fatec.qrvet.dtos.response.TutorResponse;
import ipiranga.fatec.qrvet.exceptions.InvalidRequestException;
import ipiranga.fatec.qrvet.exceptions.ResourceAlreadyExistsException;
import ipiranga.fatec.qrvet.models.Tutor;
import ipiranga.fatec.qrvet.repositories.TutorRepository;
import ipiranga.fatec.qrvet.specifications.TutorSpecifications;
import ipiranga.fatec.qrvet.utils.Cpf;
import ipiranga.fatec.qrvet.utils.PaginationUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
@PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
public class TutorService {
    private final TutorRepository repository;

    public TutorService(TutorRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TutorResponse create(TutorRequest request) {
        String cpf = Cpf.normalize(request.cpf());
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (!Cpf.isValid(cpf)) throw new InvalidRequestException("Invalid CPF.");
        if (repository.findByCpf(cpf).isPresent())
            throw new ResourceAlreadyExistsException("A tutor with this CPF already exists.");
        if (repository.findByEmailIgnoreCase(email).isPresent())
            throw new ResourceAlreadyExistsException("A tutor with this email already exists.");

        Tutor tutor = Tutor.builder()
                .nome(request.nome().trim())
                .cpf(cpf)
                .telefone(request.telefone().trim())
                .email(email)
                .endereco(request.endereco().trim())
                .build();
        return TutorResponse.from(repository.saveAndFlush(tutor));
    }

    @Transactional(readOnly = true)
    public PageResponse<TutorResponse> list(PaginationRequest pagination, TutorSearchFilter filter) {
        return PageResponse.from(repository.findAll(TutorSpecifications.matches(filter),
                PaginationUtils.byId(pagination))
                .map(TutorResponse::from));
    }
}
