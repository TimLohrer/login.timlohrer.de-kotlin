package timlohrer.de.routes

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import org.mindrot.jbcrypt.BCrypt
import timlohrer.de.database.MongoManager
import timlohrer.de.models.Account
import timlohrer.de.utils.*
import java.time.LocalDateTime
import java.time.ZoneOffset

class Auth {
    @Serializable
    data class SignInRequest(
        val email: String,
        val password: String,
        val rememberMe: Boolean = false
    )

    @Serializable
    data class SignInTokenResponse(
        val token: String
    )
    suspend fun signIn(call: ApplicationCall, mongoManager: MongoManager) {
        val body: SignInRequest = call.receive<SignInRequest>();

        val userDB = mongoManager.getCollection("users");

        val user: Account = userDB.find(Filters.eq("email", body.email)).first()?.toDataClass(fieldMappings = mapOf("id" to "_id")) ?: return badRequestError(call, "Account does not exist!");

        if (!BCrypt.checkpw(body.password, user.password)) {
            return badRequestError(call, "Incorrect password!")
        } else if (user.disabled) {
            return call.respond(HttpStatusCode.Forbidden, MessageResponse("Account disabled!"));
        } else if (user.twoFactorAuth) {
            return call.respond(HttpStatusCode.OK, MessageResponse("2fa required!"));
        }

        val token: String = JWTManager().createToken(user._id, body.rememberMe);

        userDB.findOneAndUpdate(Filters.eq("_id", user._id), Updates.set("lastLogin", LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)));

        return call.respond(HttpStatusCode.OK, SignInTokenResponse(token));
    }
}