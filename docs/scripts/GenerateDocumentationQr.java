import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import java.nio.file.Path;
import java.util.Map;

// Generates the demonstration QR with the same ZXing library used by the API.
class GenerateDocumentationQr {
    public static void main(String[] args) throws Exception {
        var matrix = new MultiFormatWriter().encode(args[0], BarcodeFormat.QR_CODE,
                300, 300, Map.of(EncodeHintType.MARGIN, 2));
        MatrixToImageWriter.writeToPath(matrix, "PNG", Path.of(args[1]));
    }
}
