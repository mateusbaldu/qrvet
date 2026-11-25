package ipiranga.fatec.qrvet.controllers;

import ipiranga.fatec.qrvet.dtos.request.EvolucaoRequest;
import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.dtos.response.EvolucaoResponse;
import ipiranga.fatec.qrvet.dtos.response.PageResponse;
import ipiranga.fatec.qrvet.services.EvolucaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/internacoes/{id}/evolucoes")
public class EvolucaoController {
    private final EvolucaoService service;
    public EvolucaoController(EvolucaoService service) { this.service = service; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public EvolucaoResponse create(@PathVariable Long id, @Valid @RequestBody EvolucaoRequest request) {
        return service.create(id, request);
    }

    @GetMapping
    public PageResponse<EvolucaoResponse> list(@PathVariable Long id, @Valid @ModelAttribute PaginationRequest pagination) {
        return service.list(id, pagination);
    }
}
