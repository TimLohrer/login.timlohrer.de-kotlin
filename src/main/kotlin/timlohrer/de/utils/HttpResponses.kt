package timlohrer.de.utils

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

@Serializable
data class MessageResponse(val message: String);

suspend fun unauthorizedError(call: ApplicationCall, errorMessage: String = "Unauthorized!") {
    return call.respond(HttpStatusCode.Unauthorized, MessageResponse(errorMessage));
}

suspend fun badRequestError(call: ApplicationCall, errorMessage: String) {
    return call.respond(HttpStatusCode.BadRequest, MessageResponse(errorMessage));
}

suspend fun internalServerError(call: ApplicationCall, errorMessage: String) {
    return call.respond(HttpStatusCode.InternalServerError, MessageResponse(errorMessage));
}