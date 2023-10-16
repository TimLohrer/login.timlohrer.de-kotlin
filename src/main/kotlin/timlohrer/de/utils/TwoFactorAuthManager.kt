package timlohrer.de.utils

import java.nio.ByteBuffer
import java.security.Key
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and

class TwoFactorAuthManager {
    val timeStep: Int = 30;
    val codeLength: Int = 6;
    fun generateSecretKey(): String {
        val random = SecureRandom();
        val secretKey = ByteArray(20);
        random.nextBytes(secretKey);
        return secretKey.toString();
    }

    fun generateTOTP(secretKey: String): String {
        val time = System.currentTimeMillis() / 1000 / timeStep;
        return generateTOTP(secretKey.toByteArray(), time);
    }

    fun validateTOTP(secretKey: String, userProvidedCode: String): Boolean {
        val currentTime = System.currentTimeMillis() / 1000 / timeStep;

        for (i in -1..1) {
            val candidateTime = currentTime + i;
            val candidateCode = generateTOTP(secretKey.toByteArray(), candidateTime);
            if (candidateCode == userProvidedCode) {
                return true;
            }
        }
        return false;
    }

    private fun generateTOTP(key: ByteArray, time: Long): String {
        val msg = ByteBuffer.allocate(8).putLong(time).array()
        val hmac = getHMAC(key, msg)
        val offset = hmac[hmac.size - 1] and 0xF

        val truncatedHash = hmac.copyOfRange(offset.toInt(), offset + 4)
        val code = ByteBuffer.wrap(truncatedHash).int and 0x7FFFFFFF % 1000000

        return code.toString().padStart(codeLength, '0')
    }

    private fun getHMAC(key: ByteArray, data: ByteArray): ByteArray {
        val signingKey: Key = SecretKeySpec(key, "HmacSHA1");
        val mac = Mac.getInstance("HmacSHA1");
        mac.init(signingKey);
        return mac.doFinal(data);
    }
}