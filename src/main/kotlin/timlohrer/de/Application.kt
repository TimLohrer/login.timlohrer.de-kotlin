package timlohrer.de

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import timlohrer.de.config.Config
import timlohrer.de.database.MongoManager
import timlohrer.de.routes.Accounts
fun main() {
    var config: Config = Config();
    var mongoManager: MongoManager = MongoManager();
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        install(ContentNegotiation) {
            json()
        }
        router(config, mongoManager);
    }.start(wait = true)
}

fun Application.router(config: Config, mongoManager: MongoManager) {
    mongoManager.connect(config);
    routing {
        route("/api") {
            post("/accounts") {
                Accounts().Create(call, mongoManager);
            }
        }
    }
}
