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
import timlohrer.de.utils.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class Accounts {
    @Serializable
    data class CreateAccountRequest(
        val firstName: String = "",
        val lastName: String = "",
        val email: String = "",
        val password: String = "",
        val confirmPassword: String = "",
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

            val userDB = mongoManager.getCollection("users");
            val adminDB = mongoManager.getCollection("admin");
            val registrationCodesDB = mongoManager.getCollection("registrationCodes");

            if (userDB.countDocuments(Filters.eq("email", body.email)) > 0) {
                return badRequestError(call, "Account with that email already exists!");
            }

            if (body.password != body.confirmPassword) {
                return badRequestError(call, "Passwords must match!");
            }

            val settings: Document =
                adminDB.find(Filters.eq("_id", "00000000-0000-0000-0000-000000000000")).singleOrNull()
                    ?: return internalServerError(call, "Failed to fetch openRegistration settings.");
            if (settings.getBoolean("openRegistration") == false) {
                registrationCodesDB.findOneAndDelete(Filters.eq("code", body.registrationCode))
                    ?: return badRequestError(call, "Invalid registration code!");
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

    suspend fun GetAll(call: ApplicationCall, mongoManager: MongoManager) {
        try {
            val userDB = mongoManager.getCollection("users");

            val accounts: MutableList<AccountResponse> = mutableListOf();
            userDB.find().toList().forEach { account: Document ->
                accounts.add(account.toDataClass(fieldMappings = mapOf("id" to "_id")));
            };

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
                userDB.findOneAndUpdate(Filters.eq("_id", id), Updates.set("twoFactorAuth", false));
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

    @Serializable
    data class AddRoleRequest(
        val role: String = ""
    );

    suspend fun AddRole(call: ApplicationCall, mongoManager: MongoManager) {
        try {
            val body: AddRoleRequest = call.receive<AddRoleRequest>();

            if (body.role.isEmpty()) {
                return badRequestError(call, "Missing required field: id");
            }

            val id: String = call.parameters["id"] ?: "";

            val userDB = mongoManager.getCollection("users");
            val roleDB = mongoManager.getCollection("roles");

            val account: Account =
                userDB.find(Filters.eq("_id", id)).first()?.toDataClass() ?: return badRequestError(
                    call, "Account does not exist!"
                );

            if (account.roles.contains(body.role)) {
                return badRequestError(call, "Account already has that role!");
            }

            roleDB.find(Filters.eq("_id", body.role)).first()
                ?: return internalServerError(call, "Role does not exist.");

            userDB.findOneAndUpdate(Filters.eq("_id", id), Updates.push("roles", body.role));

            call.respond(HttpStatusCode.OK, MessageResponse("Role added!"));
        } catch (e: Exception) {
            println(e);
            return internalServerError(call, "Error adding role to account.");
        }
    }

    @Serializable
    data class RemoveRoleRequest(
        val role: String = ""
    );

    suspend fun RemoveRole(call: ApplicationCall, mongoManager: MongoManager) {
        try {
            val body: RemoveRoleRequest = call.receive<RemoveRoleRequest>();

            if (body.role.isEmpty()) {
                return badRequestError(call, "Missing required field: id");
            }

            val id: String = call.parameters["id"] ?: "";

            val userDB = mongoManager.getCollection("users");
            val roleDB = mongoManager.getCollection("roles");

            val account: Account =
                userDB.find(Filters.eq("_id", id)).first()?.toDataClass() ?: return badRequestError(
                    call, "Account does not exist!"
                );

            if (!account.roles.contains(body.role)) {
                return badRequestError(call, "Account does not have that role!");
            }

            roleDB.find(Filters.eq("_id", body.role)).first()
                ?: return internalServerError(call, "Role does not exist.");

            userDB.findOneAndUpdate(Filters.eq("_id", id), Updates.pull("roles", body.role));

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

            var returnValue = "Disabled!";
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

            userDB.find(Filters.eq("_id", id)).first() ?: return badRequestError(
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
