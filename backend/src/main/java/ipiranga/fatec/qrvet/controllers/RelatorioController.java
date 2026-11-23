package ipiranga.fatec.qrvet.controllers;

import ipiranga.fatec.qrvet.dtos.filter.RelatorioFilter;
import ipiranga.fatec.qrvet.dtos.response.RelatorioClinicoResponse;
import ipiranga.fatec.qrvet.exceptions.InvalidRequestException;
import ipiranga.fatec.qrvet.services.RelatorioClinicoService;
import ipiranga.fatec.qrvet.utils.RelatorioClinicoWorkbook;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.ZoneId;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/relatorios")
@PreAuthorize("hasRole('ADMIN')")
public class RelatorioController {
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final RelatorioClinicoService service;

    public RelatorioController(RelatorioClinicoService service) {
        this.service = service;
    }

    @GetMapping("/clinica")
    public RelatorioClinicoResponse clinica(@ModelAttribute RelatorioFilter filter) {
        return service.gerarRelatorio(filter);
    }

    @GetMapping("/clinica/export")
    public ResponseEntity<byte[]> exportCsv(@ModelAttribute RelatorioFilter filter) {
        byte[] content = service.exportarCsv(filter);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"relatorio-performance-clinica.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(content);
    }

    @GetMapping("/clinica/export/xlsx")
    public ResponseEntity<byte[]> exportExcel(
            @ModelAttribute RelatorioFilter filter,
            @RequestParam(defaultValue = "UTC") String fuso) throws IOException {
        ZoneId zona;
        try {
            zona = ZoneId.of(fuso);
        } catch (DateTimeException e) {
            throw new InvalidRequestException("Fuso horário inválido.");
        }

        byte[] content = RelatorioClinicoWorkbook.exportar(service.gerarRelatorio(filter), zona);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"relatorio-performance-clinica.xlsx\"")
                .contentType(XLSX)
                .body(content);
    }
}
