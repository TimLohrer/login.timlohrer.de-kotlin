package timlohrer.de

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import timlohrer.de.config.Config
import timlohrer.de.database.MongoManager
import timlohrer.de.middleware.isSignedIn
import timlohrer.de.models.Account
import timlohrer.de.routes.Accounts
import timlohrer.de.routes.Auth

val mongoManager: MongoManager = MongoManager();

fun main() {
    val config: Config = Config();
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
            route("/auth") {
                post("/signin") {
                    Auth().signIn(call, mongoManager);
                }
            }
            route("/accounts") {
                put("/create") {
                    Accounts().Create(call, mongoManager);
                }
                get("/{id}") {
                    val user: Account = isSignedIn(call, mongoManager) ?: return@get;
                    Accounts().Get(call, mongoManager, user);
                }
                post("/{id}/2fa/get") {

                }
                post("/{id}/2fa") {

                }
                delete("/{id}/delete") {

                }
            }

            route("/admin") {
                route("/accounts") {
                    post("/{id}/roles/add") {

                    }
                    post("/{id}/roles/remove") {

                    }
                }

                route("/roles") {
                    put("/create") {

                    }
                    get("/{id}") {

                    }
                    get("") {

                    }
                    delete("delete") {

                    }
                }

                route("/registration-codes") {
                    put("/create") {

                    }
                    get("/") {

                    }
                    delete("/delete") {

                    }
                }
            }
        }
    }
}
