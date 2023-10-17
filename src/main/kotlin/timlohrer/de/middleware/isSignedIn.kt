package timlohrer.de.middleware

import com.auth0.jwt.interfaces.DecodedJWT
import com.mongodb.client.model.Filters
import io.ktor.server.application.*
import io.ktor.server.response.*
import timlohrer.de.database.MongoManager
import timlohrer.de.models.Account
import timlohrer.de.utils.JWTManager
import timlohrer.de.utils.badRequestError
import timlohrer.de.utils.toDataClass
import timlohrer.de.utils.unauthorizedError

suspend fun isSignedIn(call: ApplicationCall, mongoManager: MongoManager): Account? {
    var accessToken: String? = null;
    if (call.request.headers["authorization"]?.isNotEmpty() == true) {
        accessToken = call.request.headers["authorization"]?.split(" ")?.get(1);
    } else if (call.request.queryParameters["token"]?.isNotEmpty() == true) {
        accessToken = call.request.queryParameters["token"];
    }
    if (accessToken == null) {
        call.respondRedirect("/signIn", true);
        return null;
    }


    try {
        val jwtManager: JWTManager = JWTManager();
        val jwtBody: DecodedJWT? = jwtManager.verifyToken(accessToken);
        if (jwtBody == null) {
            unauthorizedError(call, "Failed to verify token!");
            return null;
        }

        val userId: String = jwtBody.getClaim("id").toString();

        val userDB = mongoManager.getCollection("users");

        val user: Account = userDB.find(Filters.eq("_id", userId.replace("\"", ""))).first().toDataClass();
        if (user.disabled) {
            badRequestError(call, "User is disabled!");
            return null;
        }
        return user;
    } catch (e: Exception) {
        println(e);
        badRequestError(call, "Failed to verify token!");
        return null;
    }
}