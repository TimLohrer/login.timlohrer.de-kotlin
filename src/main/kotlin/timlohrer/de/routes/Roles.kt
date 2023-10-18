package timlohrer.de.routes

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import org.bson.Document
import timlohrer.de.database.MongoManager
import timlohrer.de.models.Role
import timlohrer.de.utils.*
import java.util.*

class Roles {
    @Serializable
    data class RoleResponse(
        val id: String = "",
        val displayName: String = "",
        val system: Boolean = false
    );

    @Serializable
    data class CreateRoleRequest(
        val displayName: String = ""
    );

    @Serializable
    data class CreateRoleResponse(
        val message: String,
        val role: RoleResponse
    );

    suspend fun Create(call: ApplicationCall, mongoManager: MongoManager) {
        try {
            val body: CreateRoleRequest = call.receive<CreateRoleRequest>();

            if (body.displayName.isEmpty()) {
                return badRequestError(call, "Missing required fields: displayName");
            }

            val rolesDB = mongoManager.getCollection("roles");

            val role = Role(
                UUID.randomUUID().toString(),
                body.displayName,
                false
            );

            rolesDB.insertOne(role.toDocument());

            call.respond(
                HttpStatusCode.Created,
                CreateRoleResponse("Created!", RoleResponse(role._id, role.displayName, role.system))
            );
        } catch (e: Exception) {
            println(e);
            return internalServerError(call, "Error creating role.");
        }
    }

    suspend fun Get(call: ApplicationCall, mongoManager: MongoManager) {
        try {
            val id: String = call.parameters["id"] ?: "";

            val roleDB = mongoManager.getCollection("roles");

            val role: RoleResponse =
                roleDB.find(Filters.eq("_id", id)).first()
                    ?.toDataClass(fieldMappings = mapOf("id" to "_id"))
                    ?: return internalServerError(call, "Role does not exist!");

            call.respond(HttpStatusCode.OK, role);
        } catch (e: Exception) {
            println(e);
            return internalServerError(call, "Error getting role.");
        }
    }

    @Serializable
    data class GetAllRolesResponse(
        val count: Int,
        val roles: List<RoleResponse>
    );

    suspend fun GetAll(call: ApplicationCall, mongoManager: MongoManager) {
        try {
            val roleDB = mongoManager.getCollection("roles");

            val roles: MutableList<RoleResponse> = mutableListOf();
            roleDB.find().toList().forEach { role: Document ->
                roles.add(role.toDataClass(fieldMappings = mapOf("id" to "_id")));
            }

            call.respond(HttpStatusCode.OK, GetAllRolesResponse(roles.size, roles));
        } catch (e: Exception) {
            println(e);
            return internalServerError(call, "Error getting all roles.");
        }
    }

    @Serializable
    data class DeleteRoleRequest(
        val id: String = ""
    );

    suspend fun Delete(call: ApplicationCall, mongoManager: MongoManager) {
        try {
            val body: DeleteRoleRequest = call.receive<DeleteRoleRequest>();

            if (body.id.isEmpty()) {
                return badRequestError(call, "Missing required fields: id");
            }

            val userDB = mongoManager.getCollection("users");
            val roleDB = mongoManager.getCollection("roles");

            roleDB.findOneAndDelete(Filters.eq("_id", body.id));

            userDB.find().toList().forEach { user: Document ->
                userDB.findOneAndUpdate(user, Updates.pull("roles", body.id));
            };

            call.respond(HttpStatusCode.OK, MessageResponse("Deleted!"));
        } catch (e: Exception) {
            println(e);
            return internalServerError(call, "Error deleting role.");
        }
    }
}