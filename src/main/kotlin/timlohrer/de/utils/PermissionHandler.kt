package timlohrer.de.utils

import io.ktor.server.application.*
import timlohrer.de.models.Account

class SystemRoles {
    val ADMIN: String = "00000000-0000-0000-0000-000000000000";
    val DEFAULT: String = "00000000-0000-0000-0000-000000000001";
}

class PermissionHandler {
    suspend fun checkIfRequestUserOwnsResource(
        call: ApplicationCall,
        user: Account,
        resourceId: String
    ) {
        if (user._id != resourceId && !user.roles.contains(SystemRoles().ADMIN)) {
            return unauthorizedError(call, "Missing permissions to access this endpoint!");
        }
    }

    suspend fun checkIfRequestUserIdAdmin(call: ApplicationCall, user: Account) {
        if (!user.roles.contains(SystemRoles().ADMIN)) {
            return unauthorizedError(call, "Missing permissions to access this endpoint!");
        }
    }
}