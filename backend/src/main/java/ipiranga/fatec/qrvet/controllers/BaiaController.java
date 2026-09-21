package ipiranga.fatec.qrvet.controllers;

import ipiranga.fatec.qrvet.dtos.request.BaiaDetailsRequest;
import ipiranga.fatec.qrvet.dtos.request.BaiaRequest;
import ipiranga.fatec.qrvet.dtos.request.BaiaStatusRequest;
import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.dtos.response.BaiaResponse;
import ipiranga.fatec.qrvet.dtos.response.PageResponse;
import ipiranga.fatec.qrvet.models.enums.BaiaStatus;
import ipiranga.fatec.qrvet.services.BaiaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/baias")
public class BaiaController {
    private final BaiaService service;

    public BaiaController(BaiaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<BaiaResponse> create(@Valid @RequestBody BaiaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public PageResponse<BaiaResponse> list(
            @Valid @ModelAttribute PaginationRequest pagination,
            @RequestParam(required = false) BaiaStatus status) {
        return service.list(pagination, status);
    }

    @GetMapping("/{id}")
    public BaiaResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public BaiaResponse update(@PathVariable Long id, @Valid @RequestBody BaiaDetailsRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public BaiaResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody BaiaStatusRequest request) {
        return service.changeStatus(id, request);
    }
}
