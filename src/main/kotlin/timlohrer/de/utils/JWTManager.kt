package timlohrer.de.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import timlohrer.de.config.Config
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class JWTManager {
    fun createToken(userId: String, rememberMe: Boolean): String {
        val config: Config = Config();
        val now: Instant = Instant.now();
        val currentTime: Long = now.epochSecond
        var expirationDate: Long
        if (rememberMe) {
            expirationDate = now.plus(config.rememberMeTokenLifespand, ChronoUnit.DAYS).epochSecond;
        } else {
            expirationDate = now.plus(config.defaultTokenLifespand, ChronoUnit.DAYS).epochSecond;
        }
        return JWT.create()
            .withIssuedAt(Date(currentTime * 1000))
            .withExpiresAt(Date(expirationDate * 1000))
            .withIssuer("timlohrer")
            .withClaim("id", userId)
            .sign(Algorithm.HMAC256(config.JWTSecret));
    }

    fun verifyToken(token: String): DecodedJWT? {
        return try {
            JWT.require(Algorithm.HMAC256(Config().JWTSecret))
                .withIssuer("timlohrer")
                .build()
                .verify(token);
        } catch (e: Exception) {
            println(e);
            null;
        }
    }
}