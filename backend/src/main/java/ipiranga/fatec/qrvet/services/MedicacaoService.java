package ipiranga.fatec.qrvet.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ipiranga.fatec.qrvet.dtos.request.RegistroMedicacaoRequest;
import ipiranga.fatec.qrvet.dtos.response.RegistroMedicacaoResponse;
import ipiranga.fatec.qrvet.exceptions.InvalidRequestException;
import ipiranga.fatec.qrvet.exceptions.ResourceNotFoundException;
import ipiranga.fatec.qrvet.models.Internacao;
import ipiranga.fatec.qrvet.models.RegistroMedicacao;
import ipiranga.fatec.qrvet.models.enums.InternacaoStatus;
import ipiranga.fatec.qrvet.repositories.InternacaoRepository;
import ipiranga.fatec.qrvet.repositories.RegistroMedicacaoRepository;
import ipiranga.fatec.qrvet.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MedicacaoService {
    private static final Logger logger = LoggerFactory.getLogger(MedicacaoService.class);

    private final RegistroMedicacaoRepository medicacaoRepository;
    private final InternacaoRepository internacaoRepository;
    private final CurrentUser currentUser;

    public MedicacaoService(
            RegistroMedicacaoRepository medicacaoRepository,
            InternacaoRepository internacaoRepository,
            CurrentUser currentUser) {
        this.medicacaoRepository = medicacaoRepository;
        this.internacaoRepository = internacaoRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO')")
    public RegistroMedicacaoResponse registerMedication(Long internacaoId, RegistroMedicacaoRequest request) {
        logger.info("Recording medication for admission {}", internacaoId);
        Internacao internacao = internacaoRepository.findByIdForUpdate(internacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospitalization not found."));
        if (internacao.getStatus() != InternacaoStatus.ATIVA) {
            throw new InvalidRequestException("Medication can only be recorded for an active hospitalization.");
        }

        String observacoes = null;
        if (request.observacoes() != null && !request.observacoes().isBlank()) {
            observacoes = request.observacoes().trim();
        }

        RegistroMedicacao registro = RegistroMedicacao.builder()
                .internacao(internacao)
                .usuario(currentUser.getCurrent())
                .medicamento(request.medicamento().trim())
                .dose(request.dose().trim())
                .unidade(request.unidade().trim())
                .via(request.via().trim())
                .prescricao(request.prescricao().trim())
                .observacoes(observacoes)
                .build();
        return RegistroMedicacaoResponse.from(medicacaoRepository.saveAndFlush(registro));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO')")
    public List<RegistroMedicacaoResponse> medicationHistory(Long internacaoId) {
        if (!internacaoRepository.existsById(internacaoId)) {
            throw new ResourceNotFoundException("Hospitalization not found.");
        }
        return medicacaoRepository.findAllByInternacaoIdOrderByDataHoraRegistroDesc(internacaoId)
                .stream()
                .map(RegistroMedicacaoResponse::from)
                .toList();
    }
}
