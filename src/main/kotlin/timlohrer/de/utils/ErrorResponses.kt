package timlohrer.de.utils

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*


suspend fun badRequestError(call: ApplicationCall, errorMessage: String) {
    call.respond(HttpStatusCode.BadRequest, errorMessage);
}

suspend fun internalServerError(call: ApplicationCall, errorMessage: String) {
    call.respond(HttpStatusCode.InternalServerError, errorMessage);
}