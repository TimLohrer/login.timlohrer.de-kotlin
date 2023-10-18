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
import timlohrer.de.middleware.verifiedPassword
import timlohrer.de.middleware.verifiedTwoFactorAuth
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
                    val user: Account = isSignedIn(call, mongoManager) ?: return@post;
                    verifiedTwoFactorAuth(call, mongoManager);
                    Accounts().GetTwoFactorAuth(call, mongoManager, user);
                }
                post("/{id}/2fa") {
                    val user: Account = isSignedIn(call, mongoManager) ?: return@post;
                    if (user.twoFactorAuth) {
                        verifiedTwoFactorAuth(call, mongoManager);
                    } else {
                        verifiedPassword(call, mongoManager);
                    }
                    Accounts().TwoFactorAuth(call, mongoManager, user);
                }
                post("/{id}/disable") {
                    val user: Account = isSignedIn(call, mongoManager) ?: return@post;
                    Accounts().Disable(call, mongoManager, user);
                }
                delete("/{id}/delete") {
                    val user: Account = isSignedIn(call, mongoManager) ?: return@delete;
                    Accounts().Delete(call, mongoManager, user);
                }
            }

            route("/admin") {
                route("/accounts") {
                    get("/") {
                        val user: Account = isSignedIn(call, mongoManager) ?: return@get;
                        Accounts().GetAll(call, mongoManager, user);
                    }
                    post("/{id}/roles/add") {
                        val user: Account = isSignedIn(call, mongoManager) ?: return@post;
                        Accounts().AddRole(call, mongoManager, user);
                    }
                    post("/{id}/roles/remove") {
                        val user: Account = isSignedIn(call, mongoManager) ?: return@post;
                        Accounts().RemoveRole(call, mongoManager, user);
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
