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
    private final ipiranga.fatec.qrvet.services.PacienteFotoService photos;
    public PacienteController(PacienteService service, ipiranga.fatec.qrvet.services.PacienteFotoService photos) {
        this.service = service;
        this.photos = photos;
    }

    @PutMapping("/{id}")
    public PacienteResponse update(@PathVariable Long id, @Valid @RequestBody PacienteRequest request) {
        return service.update(id, request);
    }

    @PostMapping(value = "/{id}/foto", consumes = "multipart/form-data")
    public PacienteResponse upload(@PathVariable Long id, @RequestParam("foto") org.springframework.web.multipart.MultipartFile foto) {
        return photos.upload(id, foto);
    }

    @GetMapping(value = "/{id}/foto", produces = "image/jpeg")
    public ResponseEntity<byte[]> photo(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(photos.read(id));
    }

    @DeleteMapping("/{id}/foto")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePhoto(@PathVariable Long id) { photos.remove(id); }

    @PostMapping
    public ResponseEntity<PacienteResponse> create(@Valid @RequestBody PacienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public PageResponse<PacienteResponse> list(@Valid @ModelAttribute PaginationRequest pagination) {
        return service.list(pagination);
    }

    @GetMapping("/{id}")
    public PacienteResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/tutor/{tutorId}")
    public PageResponse<PacienteResponse> listByTutor(@PathVariable Long tutorId,
                                                       @Valid @ModelAttribute PaginationRequest pagination) {
        return service.listByTutor(tutorId, pagination);
    }
}
