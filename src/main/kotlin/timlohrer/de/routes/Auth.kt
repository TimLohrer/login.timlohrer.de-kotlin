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
import timlohrer.de.utils.JWTManager
import timlohrer.de.utils.MessageResponse
import timlohrer.de.utils.badRequestError
import timlohrer.de.utils.toDataClass
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

        val user: Account =
            userDB.find(Filters.eq("email", body.email)).first()?.toDataClass(fieldMappings = mapOf("id" to "_id"))
                ?: return badRequestError(call, "Account does not exist!");

        if (!BCrypt.checkpw(body.password, user.password)) {
            return badRequestError(call, "Incorrect password!")
        } else if (user.disabled) {
            return call.respond(HttpStatusCode.Forbidden, MessageResponse("Account disabled!"));
        } else if (user.twoFactorAuth) {
            return call.respond(HttpStatusCode.OK, MessageResponse("2fa required!"));
        }

        val token: String = JWTManager().createToken(user._id, body.rememberMe);

        userDB.findOneAndUpdate(
            Filters.eq("_id", user._id),
            Updates.set("lastLogin", LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
        );

        return call.respond(HttpStatusCode.OK, SignInTokenResponse(token));
    }

    @Serializable
    data class ValidateTwoFactorAuthRequestequest(
        val email: String = "",
        val password: String = "",
        val token: String = "",
        val rememberMe: Boolean = false
    )

    @Serializable
    data class ValidateTwoFactorAuthResponse(
        val token: String
    )

    suspend fun validateTwoFactorAuth(call: ApplicationCall, mongoManager: MongoManager, user: Account) {
        val body: ValidateTwoFactorAuthRequestequest = call.receive<ValidateTwoFactorAuthRequestequest>();

        val userDB = mongoManager.getCollection("users");

        val token: String = JWTManager().createToken(user._id, body.rememberMe);

        userDB.findOneAndUpdate(
            Filters.eq("_id", user._id),
            Updates.set("lastLogin", LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
        );

        return call.respond(HttpStatusCode.OK, ValidateTwoFactorAuthResponse(token));
    }
}