package ipiranga.fatec.qrvet.controllers;

import ipiranga.fatec.qrvet.dtos.request.BootstrapAdminRequest;
import ipiranga.fatec.qrvet.services.BootstrapService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/bootstrap")
public class BootstrapController {
    private final BootstrapService service;

    public BootstrapController(BootstrapService service) {
        this.service = service;
    }

    @PostMapping("/admin")
    public ResponseEntity<Object> create(
            @RequestHeader(value = "X-Bootstrap-Secret", required = false) String providedSecret,
            @Valid @RequestBody BootstrapAdminRequest request) {
        service.createAdmin(providedSecret, request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
