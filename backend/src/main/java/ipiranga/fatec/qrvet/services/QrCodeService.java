package ipiranga.fatec.qrvet.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import javax.imageio.ImageIO;

@Service
public class QrCodeService {
    private final String publicUrl;

    public QrCodeService(@Value("${qrvet.public-url}") String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public String url(UUID token) {
        return publicUrl.replaceAll("/+$", "") + "/public/internacoes/qr/" + token;
    }

    public String base64(String value) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, 400, 400);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(MatrixToImageWriter.toBufferedImage(matrix), "PNG", output)) {
                throw new IllegalStateException("QR code image format is not available.");
            }
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (WriterException | IOException exception) {
            throw new IllegalStateException("QR code could not be generated.", exception);
        }
    }
}
