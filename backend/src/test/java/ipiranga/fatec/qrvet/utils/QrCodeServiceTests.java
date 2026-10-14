package ipiranga.fatec.qrvet.utils;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class QrCodeServiceTests {
    private final QrCodeService service = new QrCodeService("https://qrvet.test///");

    @Test
    void buildsPublicAndHospitalizationUrlsWithoutTrailingSlash() {
        UUID token = UUID.fromString("11111111-1111-1111-1111-111111111111");

        assertEquals("https://qrvet.test/public/internacoes/qr/" + token, service.url(token));
        assertEquals("https://qrvet.test/internacoes/42", service.hospitalizationUrl(42L));
    }

    @Test
    void generatesPngBase64QrCode() {
        String encoded = service.base64("https://qrvet.test/internacoes/42");

        byte[] image = Base64.getDecoder().decode(encoded);
        assertTrue(image.length > 100);
        assertEquals((byte) 0x89, image[0]);
        assertEquals((byte) 0x50, image[1]);
        assertEquals((byte) 0x4e, image[2]);
        assertEquals((byte) 0x47, image[3]);
    }
}
