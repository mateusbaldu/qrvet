package ipiranga.fatec.qrvet.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ipiranga.fatec.qrvet.dtos.response.InternacaoPublicResponse;
import ipiranga.fatec.qrvet.services.InternacaoService;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/public/internacoes/qr")
public class PublicInternacaoController {
    private static final Logger logger = LoggerFactory.getLogger(PublicInternacaoController.class);
    private final InternacaoService service;

    public PublicInternacaoController(InternacaoService service) {
        this.service = service;
    }

    @GetMapping("/{uuidToken}")
    public InternacaoPublicResponse findByToken(@PathVariable UUID uuidToken) {
        return service.publicDetails(uuidToken);
    }
}
