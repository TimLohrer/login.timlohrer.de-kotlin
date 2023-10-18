package timlohrer.de.routes

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import org.bson.Document
import org.mindrot.jbcrypt.BCrypt
import timlohrer.de.database.MongoManager
import timlohrer.de.models.Account
import timlohrer.de.models.Role
import timlohrer.de.utils.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class Accounts {
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
        var id: String = "",
        var firstName: String = "",
        var lastName: String = "",
        var email: String = "",
        var twoFactorAuth: Boolean = false,
        var createdAt: Long = 0,
        var lastLogin: Long = 0,
        var roles: List<String> = emptyList(),
        var disabled: Boolean = false
    )

    suspend fun Create(call: ApplicationCall, mongoManager: MongoManager) {
        try {
            val body: CreateAccountRequest = call.receive<CreateAccountRequest>();

            if (body.firstName.isBlank() || body.lastName.isBlank() || body.email.isBlank() || body.password.isBlank()) {
                return badRequestError(call, "Missing required fields");
            }

            val adminDB = mongoManager.getCollection("admin");
            val userDB = mongoManager.getCollection("users");

            if (userDB.countDocuments(Filters.eq("email", body.email)) > 0) {
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
                LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
                LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
                listOf(SystemRoles().DEFAULT),
                false
            );

            userDB.insertOne(newAccount.toDocument());

            val settings: Document =
                adminDB.find(Filters.eq("_id", "00000000-0000-0000-0000-000000000000")).singleOrNull()
                    ?: return internalServerError(call, "Database error.");
            if (body.registrationCode.isNotEmpty() && (settings.getString("openRegistration") ?: false) == false) {
                adminDB.findOneAndUpdate(
                    Filters.eq("_id", "00000000-0000-0000-0000-000000000001"),
                    Updates.pull("registrationCodes", body.registrationCode)
                );
            }

            call.respond(HttpStatusCode.Created, "Account created successfully!");
        } catch (e: Exception) {
            println(e);
            return internalServerError(call, "Error creating the account.");
        }
    }

    suspend fun Get(call: ApplicationCall, mongoManager: MongoManager, user: Account) {
        try {
            val id: String = call.parameters["id"] ?: "";

            PermissionHandler().checkIfRequestUserOwnsResource(call, user, id);

            val userDB = mongoManager.getCollection("users");

            val account: AccountResponse =
                userDB.find(Filters.eq("_id", id)).first()?.toDataClass(fieldMappings = mapOf("id" to "_id"))
                    ?: return badRequestError(
                        call,
                        "Account does not exist!"
                    );

            call.respond(HttpStatusCode.OK, account);
        } catch (e: Exception) {
            println(e);
            return internalServerError(call, "Error getting account.");
        }
    }

    @Serializable
    data class GetAllAccountsResponse(
        val count: Int,
        val users: List<AccountResponse>
    );

    suspend fun GetAll(call: ApplicationCall, mongoManager: MongoManager, user: Account) {
        try {
            PermissionHandler().checkIfRequestUserIdAdmin(call, user);

            val userDB = mongoManager.getCollection("users");

            val documents: List<Document> = userDB.find().toList();

            var accounts: MutableList<AccountResponse> = mutableListOf();
            for (document in documents) {
                accounts.add(document.toDataClass(fieldMappings = mapOf("id" to "_id")));
            }

            call.respond(HttpStatusCode.OK, GetAllAccountsResponse(accounts.size, accounts));
        } catch (e: Exception) {
            println(e);
            return internalServerError(call, "Error getting all accounts.");
        }
    }

    suspend fun TwoFactorAuth(call: ApplicationCall, mongoManager: MongoManager, user: Account) {
        try {
            val id: String = call.parameters["id"] ?: "";

            PermissionHandler().checkIfRequestUserOwnsResource(call, user, id);

            val userDB = mongoManager.getCollection("users");

            val account: Account =
                userDB.find(Filters.eq("_id", id)).first()?.toDataClass() ?: return badRequestError(
                    call, "Account does not exist!"
                );

            if (!account.twoFactorAuth) {
                userDB.findOneAndUpdate(Filters.eq("_id", id), Updates.set("twoFactorAuth", true));

                return call.respond(
                    HttpStatusCode.OK,
                    GetTwoFactorAuthResponse(
                        true,
                        account.twoFactorAuthKey,
                        TwoFactorAuthManager().generateQrCode(account.email, account.twoFactorAuthKey)
                    )
                );
            } else {
                userDB.findOneAndUpdate(Filters.eq("_id", id), Updates.set("twoFactorAuth", true));
                userDB.findOneAndUpdate(
                    Filters.eq("_id", id),
                    Updates.set("twoFactorAuthKey", TwoFactorAuthManager().generateSecretKey())
                );

                return call.respond(HttpStatusCode.OK);
            }
        } catch (e: Exception) {
            println(e);
            return internalServerError(call, "Error managing 2fa settings.");
        }
    }

    @Serializable
    data class GetTwoFactorAuthResponse(
        val enabled: Boolean,
        val key: String,
        val qrCode: String
    )

    suspend fun GetTwoFactorAuth(call: ApplicationCall, mongoManager: MongoManager, user: Account) {
        try {
            val id: String = call.parameters["id"] ?: "";

            PermissionHandler().checkIfRequestUserOwnsResource(call, user, id);

            val userDB = mongoManager.getCollection("users");

            val account: Account =
                userDB.find(Filters.eq("_id", id)).first()?.toDataClass() ?: return badRequestError(
                    call, "Account does not exist!"
                );

            if (!account.twoFactorAuth) {
                return badRequestError(call, "2fa is not enabled on this account!");
            }

            call.respond(
                HttpStatusCode.OK,
                GetTwoFactorAuthResponse(
                    account.twoFactorAuth,
                    account.twoFactorAuthKey,
                    TwoFactorAuthManager().generateQrCode(account.email, account.twoFactorAuthKey)
                )
            );
        } catch (e: Exception) {
            println(e);
            return internalServerError(call, "Error setting up 2fa.");
        }
    }

    data class Roles(
        val roles: List<Document> = listOf()
    );

    @Serializable
    data class AddRoleRequest(
        val id: String = ""
    );

    suspend fun AddRole(call: ApplicationCall, mongoManager: MongoManager, user: Account) {
        try {
            val body: AddRoleRequest = call.receive<AddRoleRequest>();

            if (body.id.isEmpty()) {
                return badRequestError(call, "Missing required field: id");
            }

            val id: String = call.parameters["id"] ?: "";

            PermissionHandler().checkIfRequestUserIdAdmin(call, user);

            val userDB = mongoManager.getCollection("users");
            val adminDB = mongoManager.getCollection("admin");

            val account: Account =
                userDB.find(Filters.eq("_id", id)).first()?.toDataClass() ?: return badRequestError(
                    call, "Account does not exist!"
                );

            if (account.roles.contains(body.id)) {
                return badRequestError(call, "Account already has that role!");
            }

            val roles: Roles =
                adminDB.find(Filters.eq("_id", "00000000-0000-0000-0000-000000000002")).first()?.toDataClass<Roles>()
                    ?: return internalServerError(call, "Failed to fetch role.");

            if (!roles.roles.any { it.toDataClass<Role>().id == body.id }) {
                return badRequestError(call, "Role does not exist!");
            }

            userDB.findOneAndUpdate(Filters.eq("_id", id), Updates.push("roles", body.id));

            call.respond(HttpStatusCode.OK, MessageResponse("Role added!"));
        } catch (e: Exception) {
            println(e);
            return internalServerError(call, "Error adding role to account.");
        }
    }

    @Serializable
    data class RemoveRoleRequest(
        val id: String = ""
    );

    suspend fun RemoveRole(call: ApplicationCall, mongoManager: MongoManager, user: Account) {
        try {
            val body: RemoveRoleRequest = call.receive<RemoveRoleRequest>();

            if (body.id.isEmpty()) {
                return badRequestError(call, "Missing required field: id");
            }

            val id: String = call.parameters["id"] ?: "";

            PermissionHandler().checkIfRequestUserIdAdmin(call, user);

            val userDB = mongoManager.getCollection("users");
            val adminDB = mongoManager.getCollection("admin");

            val account: Account =
                userDB.find(Filters.eq("_id", id)).first()?.toDataClass() ?: return badRequestError(
                    call, "Account does not exist!"
                );

            if (!account.roles.contains(body.id)) {
                return badRequestError(call, "Account does not have that role!");
            }

            val roles: Roles =
                adminDB.find(Filters.eq("_id", "00000000-0000-0000-0000-000000000002")).first()?.toDataClass<Roles>()
                    ?: return internalServerError(call, "Failed to fetch role.");

            if (!roles.roles.any { it.toDataClass<Role>().id == body.id }) {
                return badRequestError(call, "Role does not exist!");
            }

            userDB.findOneAndUpdate(Filters.eq("_id", id), Updates.pull("roles", body.id));

            call.respond(HttpStatusCode.OK, MessageResponse("Role removed!"));
        } catch (e: Exception) {
            println(e);
            return internalServerError(call, "Error removing role from account.");
        }
    }

    suspend fun Disable(call: ApplicationCall, mongoManager: MongoManager, user: Account) {
        try {
            val id: String = call.parameters["id"] ?: "";

            if (user.disabled) {
                PermissionHandler().checkIfRequestUserIdAdmin(call, user, "Only Admins can enable accounts!");
            } else {
                PermissionHandler().checkIfRequestUserOwnsResource(call, user, id);
            }

            val userDB = mongoManager.getCollection("users");

            val account: Account =
                userDB.find(Filters.eq("_id", id)).first()?.toDataClass() ?: return badRequestError(
                    call, "Account does not exist!"
                );

            userDB.findOneAndUpdate(Filters.eq("_id", id), Updates.set("disabled", !account.disabled));

            var returnValue: String = "Disabled!";
            if (account.disabled) {
                returnValue = "Enabled!";
            }
            return call.respond(HttpStatusCode.OK, MessageResponse(returnValue));
        } catch (e: Exception) {
            println(e);
            return internalServerError(call, "Error toggeling disable for this account.");
        }
    }

    suspend fun Delete(call: ApplicationCall, mongoManager: MongoManager, user: Account) {
        try {
            val id: String = call.parameters["id"] ?: "";

            PermissionHandler().checkIfRequestUserOwnsResource(call, user, id);

            val userDB = mongoManager.getCollection("users");

            val account: Account =
                userDB.find(Filters.eq("_id", id)).first()?.toDataClass() ?: return badRequestError(
                    call, "Account does not exist!"
                );

            userDB.findOneAndDelete(Filters.eq("_id", id));

            return call.respond(HttpStatusCode.OK, MessageResponse("Deleted!"));
        } catch (e: Exception) {
            println(e);
            return internalServerError(call, "Error deleting account.");
        }
    }
}
