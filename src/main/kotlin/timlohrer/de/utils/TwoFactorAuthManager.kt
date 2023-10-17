package timlohrer.de.utils

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.common.BitMatrix
import dev.turingcomplete.kotlinonetimepassword.GoogleAuthenticator
import org.apache.commons.codec.binary.Base32
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.*

class TwoFactorAuthManager {
    val timeStep: Int = 30;
    val codeLength: Int = 6;

    fun generateSecretKey(): String {
        val random = SecureRandom();
        val secretKey = ByteArray(48);
        random.nextBytes(secretKey);
        return Base32(false).encodeAsString(secretKey).uppercase();
    }

    fun generateQrCode(email: String, secretKey: String): String {
        val matrix: BitMatrix = MultiFormatWriter().encode(
            "otpauth://totp/TimLohrer:$email?secret=$secretKey",
            BarcodeFormat.QR_CODE,
            256,
            256
        );

        val outputStream = ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
        val imageBytes: ByteArray = outputStream.toByteArray();
        val base64Image: String = Base64.getEncoder().encodeToString(imageBytes);

        return "data:image/png;base64,$base64Image";
    }

    fun validateTOTP(secretKey: String, userProvidedCode: String): Boolean {
        return GoogleAuthenticator(secretKey.toByteArray()).isValid(userProvidedCode);
    }
}