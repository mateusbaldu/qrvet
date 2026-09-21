package ipiranga.fatec.qrvet.controllers;

import ipiranga.fatec.qrvet.dtos.request.PacienteRequest;
import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.dtos.response.PacienteResponse;
import ipiranga.fatec.qrvet.dtos.response.PageResponse;
import ipiranga.fatec.qrvet.services.PacienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/pacientes")
public class PacienteController {
    private final PacienteService service;
    public PacienteController(PacienteService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<PacienteResponse> create(@Valid @RequestBody PacienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public PageResponse<PacienteResponse> list(@Valid @ModelAttribute PaginationRequest pagination) {
        return service.list(pagination);
    }

    @GetMapping("/tutor/{tutorId}")
    public PageResponse<PacienteResponse> listByTutor(@PathVariable Long tutorId,
                                                       @Valid @ModelAttribute PaginationRequest pagination) {
        return service.listByTutor(tutorId, pagination);
    }
}
