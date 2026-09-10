package ipiranga.fatec.qrvet.controllers;

import ipiranga.fatec.qrvet.dtos.request.EncerramentoInternacaoRequest;
import ipiranga.fatec.qrvet.dtos.request.InternacaoRequest;
import ipiranga.fatec.qrvet.dtos.request.JejumRequest;
import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.dtos.request.RegistroAlimentacaoRequest;
import ipiranga.fatec.qrvet.dtos.response.JejumResponse;
import ipiranga.fatec.qrvet.dtos.response.InternacaoQrCodeResponse;
import ipiranga.fatec.qrvet.dtos.response.InternacaoResponse;
import ipiranga.fatec.qrvet.dtos.response.PageResponse;
import ipiranga.fatec.qrvet.dtos.response.RegistroAlimentacaoResponse;
import ipiranga.fatec.qrvet.services.InternacaoService;
import ipiranga.fatec.qrvet.services.JejumService;
import ipiranga.fatec.qrvet.services.RegistroAlimentacaoService;
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
    private final RegistroAlimentacaoService alimentacaoService;

    public InternacaoController(
            InternacaoService service,
            JejumService jejumService,
            RegistroAlimentacaoService alimentacaoService) {
        this.service = service;
        this.jejumService = jejumService;
        this.alimentacaoService = alimentacaoService;
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

    @PostMapping("/{id}/alimentacao")
    public ResponseEntity<RegistroAlimentacaoResponse> registerFood(
            @PathVariable Long id,
            @Valid @RequestBody RegistroAlimentacaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(alimentacaoService.create(id, request));
    }

    @GetMapping("/{id}/alimentacao")
    public List<RegistroAlimentacaoResponse> foodHistory(@PathVariable Long id) {
        return alimentacaoService.history(id);
    }
}
