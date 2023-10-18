package timlohrer.de.routes

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import timlohrer.de.database.MongoManager
import timlohrer.de.models.Setting
import timlohrer.de.utils.internalServerError
import timlohrer.de.utils.toDataClass

class Settings {
    suspend fun Get(call: ApplicationCall, mongoManager: MongoManager) {
        try {
            val adminDB = mongoManager.getCollection("admin");

            val settings: Setting =
                adminDB.find(Filters.eq("_id", "00000000-0000-0000-0000-000000000000")).first()
                    ?.toDataClass(excludeFields = setOf("_id"))
                    ?: return internalServerError(call, "Failed to fetch settings!");

            call.respond(HttpStatusCode.OK, settings);
        } catch (e: Exception) {
            println(e);
            return internalServerError(call, "Error getting settings.");
        }
    }

    suspend fun Update(call: ApplicationCall, mongoManager: MongoManager) {
        try {
            val body: Setting = call.receive<Setting>();

            val adminDB = mongoManager.getCollection("admin");

            var settings: Setting =
                adminDB.find(Filters.eq("_id", "00000000-0000-0000-0000-000000000000")).first()
                    ?.toDataClass(excludeFields = setOf("_id"))
                    ?: return internalServerError(call, "Failed to fetch settings!");

            if (body.openRegistration != null) {
                adminDB.findOneAndUpdate(
                    Filters.eq("_id", "00000000-0000-0000-0000-000000000000"),
                    Updates.set("openRegistration", body.openRegistration)
                );
            }

            call.respond(HttpStatusCode.OK, settings);
        } catch (e: Exception) {
            println(e);
            return internalServerError(call, "Error updating settings.");
        }
    }
}