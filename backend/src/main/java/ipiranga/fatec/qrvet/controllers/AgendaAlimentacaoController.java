package ipiranga.fatec.qrvet.controllers;

import ipiranga.fatec.qrvet.dtos.request.AgendaAlimentacaoRequest;
import ipiranga.fatec.qrvet.dtos.response.AgendaAlimentacaoResponse;
import ipiranga.fatec.qrvet.services.AgendaAlimentacaoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/v1")
public class AgendaAlimentacaoController {
    private final AgendaAlimentacaoService service;
    public AgendaAlimentacaoController(AgendaAlimentacaoService service) { this.service = service; }
    public record ConclusaoRequest(@Size(max = 10000) String observacao) {}
    public record CancelamentoRequest(@NotBlank @Size(max = 255) String motivo) {}

    @GetMapping("/alimentacao/agenda")
    public List<AgendaAlimentacaoResponse> list(@RequestParam Instant inicio, @RequestParam Instant fim,
                                               @RequestParam(required = false) Long internacaoId) {
        return service.checklist(inicio, fim, internacaoId);
    }
    @PostMapping("/internacoes/{id}/alimentacao/agenda") @ResponseStatus(HttpStatus.CREATED)
    public List<AgendaAlimentacaoResponse> create(@PathVariable Long id, @Valid @RequestBody AgendaAlimentacaoRequest request) {
        return service.create(id, request);
    }
    @PutMapping("/internacoes/{id}/alimentacao/agenda/{agendaId}/concluir")
    public AgendaAlimentacaoResponse complete(@PathVariable Long id, @PathVariable Long agendaId, @Valid @RequestBody ConclusaoRequest request) {
        return service.complete(id, agendaId, request.observacao());
    }
    @PutMapping("/internacoes/{id}/alimentacao/agenda/{agendaId}/cancelar")
    public AgendaAlimentacaoResponse cancel(@PathVariable Long id, @PathVariable Long agendaId, @Valid @RequestBody CancelamentoRequest request) {
        return service.cancel(id, agendaId, request.motivo());
    }
}
