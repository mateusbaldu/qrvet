package ipiranga.fatec.qrvet.controllers;

import ipiranga.fatec.qrvet.services.PacienteFotoService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/internacoes/{id}/foto")
public class InternacaoFotoController {
    private final PacienteFotoService photos;
    public InternacaoFotoController(PacienteFotoService photos) { this.photos = photos; }

    @GetMapping(produces = "image/jpeg")
    public ResponseEntity<byte[]> photo(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(photos.readHospitalization(id));
    }
}
