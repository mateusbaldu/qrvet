package ipiranga.fatec.qrvet.controllers;

import ipiranga.fatec.qrvet.dtos.filter.TutorSearchFilter;
import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.dtos.request.TutorRequest;
import ipiranga.fatec.qrvet.dtos.response.PageResponse;
import ipiranga.fatec.qrvet.dtos.response.TutorResponse;
import ipiranga.fatec.qrvet.services.TutorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/tutores")
public class TutorController {
    private final TutorService service;
    public TutorController(TutorService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<TutorResponse> create(@Valid @RequestBody TutorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public PageResponse<TutorResponse> list(@Valid @ModelAttribute PaginationRequest pagination,
                                             @ModelAttribute TutorSearchFilter filter) {
        return service.list(pagination, filter);
    }
}
