package timlohrer.de.middleware

import com.auth0.jwt.interfaces.DecodedJWT
import com.mongodb.client.model.Filters
import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.serialization.Serializable
import org.mindrot.jbcrypt.BCrypt
import timlohrer.de.database.MongoManager
import timlohrer.de.models.Account
import timlohrer.de.utils.JWTManager
import timlohrer.de.utils.badRequestError
import timlohrer.de.utils.toDataClass
import timlohrer.de.utils.unauthorizedError

@Serializable
data class VerifiedPassword(
    val id: String = "",
    val email: String = "",
    val password: String = "",
    val token: String = ""
)

suspend fun verifiedPassword(call: ApplicationCall, mongoManager: MongoManager) {
    try {
        val body: VerifiedPassword = call.receive<VerifiedPassword>();

        var accessToken: String? = null;
        if (call.request.headers["authorization"]?.isNotEmpty() == true) {
            accessToken = call.request.headers["authorization"]?.split(" ")?.get(1);
        } else if (call.request.queryParameters["token"]?.isNotEmpty() == true) {
            accessToken = call.request.queryParameters["token"];
        }
        if (accessToken == null) {
            return unauthorizedError(call);
        }

        val jwtManager: JWTManager = JWTManager();
        val jwtBody: DecodedJWT = jwtManager.verifyToken(accessToken)
            ?: return unauthorizedError(call, "Failed to verify token!");

        val userId: String = jwtBody.getClaim("id").toString().replace("\"", "");

        val userDB = mongoManager.getCollection("users");

        val user: Account = userDB.find(Filters.eq("_id", userId)).first().toDataClass();
        if (user.disabled) {
            return badRequestError(call, "User is disabled!");
        }

        if (!BCrypt.checkpw(body.password, user.password)) {
            return unauthorizedError(call, "Incorrect password!");
        }
    } catch (e: Exception) {
        println(e);
        return badRequestError(call, "Failed to verify password!");
    }
}