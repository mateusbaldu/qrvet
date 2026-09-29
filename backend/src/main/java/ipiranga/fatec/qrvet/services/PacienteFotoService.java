package ipiranga.fatec.qrvet.services;

import ipiranga.fatec.qrvet.dtos.response.PacienteResponse;
import ipiranga.fatec.qrvet.exceptions.InvalidRequestException;
import ipiranga.fatec.qrvet.exceptions.ResourceNotFoundException;
import ipiranga.fatec.qrvet.repositories.PacienteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@PreAuthorize("@qrvetSecurity.hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
public class PacienteFotoService {
    private final PacienteRepository patients;
    private final Path directory;

    public PacienteFotoService(PacienteRepository patients, @Value("${qrvet.upload-dir:./uploads}") String directory) {
        this.patients = patients;
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    @Transactional
    public PacienteResponse upload(Long id, MultipartFile file) {
        var patient = patients.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Patient not found."));
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
            throw new InvalidRequestException("Escolha uma foto JPG ou PNG de até 5 MB.");
        String name = UUID.randomUUID() + ".jpg";
        try (var input = ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(file.getBytes()))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalidImage();
            var reader = readers.next();
            try {
                reader.setInput(input);
                String format = reader.getFormatName();
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (!(format.equalsIgnoreCase("JPEG") || format.equalsIgnoreCase("PNG")) || width < 1 || height < 1
                        || (long) width * height > 16_000_000) throw invalidImage();
                BufferedImage original = reader.read(0);
                double scale = Math.min(1, 1280.0 / Math.max(width, height));
                var resized = new BufferedImage(Math.max(1, (int) (width * scale)), Math.max(1, (int) (height * scale)), BufferedImage.TYPE_INT_RGB);
                var graphics = resized.createGraphics();
                try {
                    graphics.setColor(Color.WHITE);
                    graphics.fillRect(0, 0, resized.getWidth(), resized.getHeight());
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    graphics.drawImage(original, 0, 0, resized.getWidth(), resized.getHeight(), null);
                } finally { graphics.dispose(); }
                Files.createDirectories(directory);
                cleanupAfterTransaction(patient.getFotoArquivo(), name);
                ImageIO.write(resized, "jpg", directory.resolve(name).toFile());
            } finally { reader.dispose(); }
        } catch (javax.imageio.IIOException e) { throw invalidImage(); }
        catch (IOException e) { throw new UncheckedIOException("Não foi possível armazenar a foto.", e); }
        patient.setFotoArquivo(name);
        return PacienteResponse.from(patients.saveAndFlush(patient));
    }

    @Transactional(readOnly = true)
    public byte[] read(Long id) {
        var patient = patients.findById(id).orElseThrow(() -> new ResourceNotFoundException("Patient not found."));
        if (patient.getFotoArquivo() == null) throw new ResourceNotFoundException("Paciente sem foto.");
        try { return Files.readAllBytes(directory.resolve(patient.getFotoArquivo())); }
        catch (java.nio.file.NoSuchFileException e) { throw new ResourceNotFoundException("Foto não encontrada no armazenamento local."); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

    @Transactional
    public void remove(Long id) {
        var patient = patients.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Patient not found."));
        cleanupAfterTransaction(patient.getFotoArquivo(), null);
        patient.setFotoArquivo(null);
    }

    private InvalidRequestException invalidImage() {
        return new InvalidRequestException("Foto inválida. Use JPG ou PNG com até 16 megapixels.");
    }

    private void cleanupAfterTransaction(String previous, String uploaded) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                String remove = status == STATUS_COMMITTED ? previous : uploaded;
                if (remove != null) try { Files.deleteIfExists(directory.resolve(remove)); }
                catch (IOException e) { org.slf4j.LoggerFactory.getLogger(PacienteFotoService.class).warn("Falha ao limpar foto substituída", e); }
            }
        });
    }
}
