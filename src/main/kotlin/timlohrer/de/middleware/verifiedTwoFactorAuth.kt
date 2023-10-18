package timlohrer.de.middleware

import com.auth0.jwt.interfaces.DecodedJWT
import com.mongodb.client.model.Filters
import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.serialization.Serializable
import org.bson.conversions.Bson
import timlohrer.de.database.MongoManager
import timlohrer.de.models.Account
import timlohrer.de.utils.*

@Serializable
data class VerifiedTwoFactorAuth(
    val id: String = "",
    val email: String = "",
    val password: String = "",
    val token: String = "",
    val rememberMe: Boolean = false,
    val enabled: Boolean = false
)

suspend fun verifiedTwoFactorAuth(call: ApplicationCall, mongoManager: MongoManager) {
    try {
        val body: VerifiedTwoFactorAuth = call.receive<VerifiedTwoFactorAuth>();
        var filter: Bson? = null;

        var accessToken: String? = null;
        if (call.request.headers["authorization"]?.isNotEmpty() == true) {
            accessToken = call.request.headers["authorization"]?.split(" ")?.get(1);
        } else if (call.request.queryParameters["token"]?.isNotEmpty() == true) {
            accessToken = call.request.queryParameters["token"];
        }

        if (body.id.isNotEmpty()) {
            filter = Filters.eq("_id", body.id);
        } else if (accessToken != null) {
            val jwtManager: JWTManager = JWTManager();
            val jwtBody: DecodedJWT = jwtManager.verifyToken(accessToken)
                ?: return unauthorizedError(call, "Failed to verify token!");

            val userId: String = jwtBody.getClaim("id").toString().replace("\"", "");

            filter = Filters.eq("_id", userId);
        } else if (body.email.isNotEmpty()) {
            filter = Filters.eq("email", body.email);
        }

        val userDB = mongoManager.getCollection("users");

        val user: Account = userDB.find(filter).first().toDataClass();
        if (user.disabled) {
            return badRequestError(call, "User is disabled!");
        } else if (!user.twoFactorAuth) {
            return unauthorizedError(call, "This endpoint requires 2fa!");
        } else if (body.token.isEmpty()) {
            return badRequestError(call, "Missing required field: token!");
        }

        val valid: Boolean = TwoFactorAuthManager().validateTOTP(user.twoFactorAuthKey, body.token);
        if (!valid) {
            return unauthorizedError(call, "Incorrect 2fa token!");
        }
    } catch (e: Exception) {
        println(e);
        return internalServerError(call, "Failed to verify token!");
    }
}