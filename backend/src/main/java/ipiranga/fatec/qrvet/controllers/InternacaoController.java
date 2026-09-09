package ipiranga.fatec.qrvet.controllers;

import ipiranga.fatec.qrvet.dtos.request.EncerramentoInternacaoRequest;
import ipiranga.fatec.qrvet.dtos.request.InternacaoRequest;
import ipiranga.fatec.qrvet.dtos.request.JejumRequest;
import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.dtos.response.JejumResponse;
import ipiranga.fatec.qrvet.dtos.response.InternacaoQrCodeResponse;
import ipiranga.fatec.qrvet.dtos.response.InternacaoResponse;
import ipiranga.fatec.qrvet.dtos.response.PageResponse;
import ipiranga.fatec.qrvet.services.InternacaoService;
import ipiranga.fatec.qrvet.services.JejumService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/v1/internacoes")
public class InternacaoController {
    private final InternacaoService service;
    private final JejumService jejumService;

    public InternacaoController(InternacaoService service, JejumService jejumService) {
        this.service = service;
        this.jejumService = jejumService;
    }

    @PostMapping
    public ResponseEntity<InternacaoResponse> open(@Valid @RequestBody InternacaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.open(request));
    }

    @GetMapping("/ativas")
    public PageResponse<InternacaoResponse> listActive(
            @Valid @ModelAttribute PaginationRequest pagination) {
        return service.listActive(pagination);
    }

    @GetMapping("/{id}")
    public InternacaoResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/{id}/qrcode")
    public InternacaoQrCodeResponse qrCode(@PathVariable Long id) {
        return service.qrCode(id);
    }

    @PutMapping("/{id}/encerrar")
    public InternacaoResponse close(
            @PathVariable Long id,
            @Valid @RequestBody EncerramentoInternacaoRequest request) {
        return service.close(id, request);
    }

    @PostMapping("/{id}/jejum")
    public ResponseEntity<JejumResponse> startFasting(
            @PathVariable Long id,
            @Valid @RequestBody JejumRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jejumService.start(id, request));
    }

    @PutMapping("/{id}/jejum/encerrar")
    public JejumResponse endFasting(@PathVariable Long id) {
        return jejumService.end(id);
    }

    @GetMapping("/{id}/jejum")
    public List<JejumResponse> fastingHistory(@PathVariable Long id) {
        return jejumService.history(id);
    }
}
