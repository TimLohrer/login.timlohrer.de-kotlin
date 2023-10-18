package timlohrer.de.routes

import com.mongodb.client.model.Filters
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import org.bson.Document
import timlohrer.de.database.MongoManager
import timlohrer.de.models.RegistrationCode
import timlohrer.de.utils.*
import kotlin.random.Random

class RegistrationCodes {
    @Serializable
    data class CreateRegistrationCodeResponse(
        val message: String,
        val registrationCode: String
    );
    suspend fun Create(call: ApplicationCall, mongoManager: MongoManager) {
        try {
            val registrationCodesDB = mongoManager.getCollection("registrationCodes");

            val code: String = Random.nextInt(100, 1000).toString() + "-" + Random.nextInt(100, 1000).toString();

            val registrationCode = RegistrationCode(code);

            registrationCodesDB.insertOne(registrationCode.toDocument());

            return call.respond(
                HttpStatusCode.Created,
                CreateRegistrationCodeResponse("Created!", registrationCode.code)
            );
        } catch (e: Exception) {
            println(e);
            return internalServerError(call, "Error creating registration-code.");
        }
    }

    @Serializable
    data class ValidateRegistrationCodeRequest(
        val registrationCode: String = ""
    );
    suspend fun Validate(call: ApplicationCall, mongoManager: MongoManager) {
        try {
            val body: ValidateRegistrationCodeRequest = call.receive<ValidateRegistrationCodeRequest>();

            if (body.registrationCode.isEmpty()) {
                return badRequestError(call, "Missing required fields: registrationCode");
            }

            val registrationCodesDB = mongoManager.getCollection("registrationCodes");

            registrationCodesDB.find(Filters.eq("code", body.registrationCode)).first() ?: return badRequestError(
                call,
                "Invalid registration code!"
            );

            return call.respond(HttpStatusCode.OK, MessageResponse("Valid!"));
        } catch (e: Exception) {
            println(e);
            return internalServerError(call, "Error creating registration-code.");
        }
    }

    @Serializable
    data class GetAllRegistrationCodesResponse(
        val length: Int,
        val registrationCodes: List<String>
    );
    suspend fun GetAll(call: ApplicationCall, mongoManager: MongoManager) {
        try {
            val registrationCodesDB = mongoManager.getCollection("registrationCodes");

            val registrationCodes: MutableList<String> = mutableListOf();
            registrationCodesDB.find().toList().forEach { registrationCode: Document ->
                registrationCodes.add(registrationCode.toDataClass<RegistrationCode>(excludeFields = setOf("_id")).code);
            }

            return call.respond(
                HttpStatusCode.OK,
                GetAllRegistrationCodesResponse(registrationCodes.size, registrationCodes)
            );
        } catch (e: Exception) {
            println(e);
            return internalServerError(call, "Error creating registration-code.");
        }
    }

    @Serializable
    data class DeleteRegistrationCodeRequest(
        val registrationCode: String
    );
    suspend fun Delete(call: ApplicationCall, mongoManager: MongoManager) {
        try {
            val body: DeleteRegistrationCodeRequest = call.receive<DeleteRegistrationCodeRequest>();

            if (body.registrationCode.isEmpty()) {
                return badRequestError(call, "Missing required fields: registrationCode");
            }

            val registrationCodesDB = mongoManager.getCollection("registrationCodes");

            registrationCodesDB.findOneAndDelete(Filters.eq("code", body.registrationCode));

            return call.respond(HttpStatusCode.OK, MessageResponse("Deleted!"));
        } catch (e: Exception) {
            println(e);
            return internalServerError(call, "Error creating registration-code.");
        }
    }
}