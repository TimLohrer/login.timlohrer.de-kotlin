package timlohrer.de.routes

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import java.util.UUID;
import timlohrer.de.database.MongoManager
import timlohrer.de.models.Account
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.Serializable
import org.bson.Document
import timlohrer.de.utils.TwoFactorAuthManager
import timlohrer.de.utils.badRequestError
import timlohrer.de.utils.internalServerError
import timlohrer.de.utils.toDocument

@Serializable
data class CreateAccountRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val registrationCode: String = ""
);

@Serializable
data class AccountResponse(
    var id: String,
    var firstName: String,
    var lastName: String,
    var email: String,
    var twoFactorAuth: Boolean,
    var createdAt: String,
    var lastLogin: String,
    var roles: List<String>,
    var disabled: Boolean
)

class Accounts {
    suspend fun Create(call: ApplicationCall, mongoManager: MongoManager) {
        try {
            val body = call.receive<CreateAccountRequest>();

            if (body.firstName.isBlank() || body.lastName.isBlank() || body.email.isBlank() || body.password.isBlank()) {
                return badRequestError(call, "Missing required fields");
            }

            val adminDB = mongoManager.getCollection("admin");
            val userDB = mongoManager.getCollection("users");

            if (userDB.countDocuments(Document("email", body.email)) > 0) {
                return badRequestError(call, "Account with that email already exists!");
            }

            val id: String = UUID.randomUUID().toString();
            val twoFactorSecret: String = TwoFactorAuthManager().generateSecretKey();

            val newAccount = Account(
                id,
                body.firstName,
                body.lastName,
                body.email,
                BCrypt.hashpw(body.password, BCrypt.gensalt(10)),
                false,
                twoFactorSecret,
                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                "",
                listOf("00000000-0000-0000-0000-000000000001"),
                false
            );

            userDB.insertOne(newAccount.toDocument());

            val settings: Document = adminDB.find(Filters.eq("_id", "00000000-0000-0000-0000-000000000000")).singleOrNull()
                ?: return internalServerError(call, "Database error.");
            if (body.registrationCode.isNotEmpty() && (settings.getString("openRegistration") ?: false) == false) {
                adminDB.findOneAndUpdate(Filters.eq("_id", "00000000-0000-0000-0000-000000000001"), Updates.pull("registrationCodes", body.registrationCode));
            }

            call.respond(HttpStatusCode.Created, "Account created successfully!");
        } catch (e: Exception) {
            println(e);
            return internalServerError(call, "Error creating the account.");
        }
    }
}
