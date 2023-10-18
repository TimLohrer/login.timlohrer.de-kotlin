package timlohrer.de

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import timlohrer.de.config.Config
import timlohrer.de.database.MongoManager
import timlohrer.de.middleware.isSignedIn
import timlohrer.de.middleware.verifiedPassword
import timlohrer.de.middleware.verifiedTwoFactorAuth
import timlohrer.de.models.Account
import timlohrer.de.routes.*
import timlohrer.de.utils.MessageResponse
import timlohrer.de.utils.PermissionHandler

val mongoManager: MongoManager = MongoManager();

fun main() {
    val config = Config();
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        install(CORS) {
            allowHost("timlohrer.de", listOf("https", "http"), listOf("login"))
            allowHeader("authorization")
            allowHeader("content-type");
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Delete)
            allowMethod(HttpMethod.Options)
            allowCredentials = true;
        }
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
                post("/2fa/validate") {
                    val user: Account = isSignedIn(call, mongoManager) ?: return@post;
                    verifiedTwoFactorAuth(call, mongoManager);
                    Auth().validateTwoFactorAuth(call, mongoManager, user);
                }
                post("/validate") {
                    isSignedIn(call, mongoManager) ?: return@post;
                    return@post call.respond(HttpStatusCode.OK, MessageResponse("Valid!"));
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

            get("/roles/{id}") {
                isSignedIn(call, mongoManager) ?: return@get;
                Roles().Get(call, mongoManager);
            }

            get("/registration-codes/validate") {
                RegistrationCodes().Validate(call, mongoManager);
            }

            route("/admin") {
                get("/accounts") {
                    val user: Account = isSignedIn(call, mongoManager) ?: return@get;
                    PermissionHandler().checkIfRequestUserIdAdmin(call, user);
                    Accounts().GetAll(call, mongoManager);
                }
                route("/accounts") {
                    post("/{id}/roles/add") {
                        val user: Account = isSignedIn(call, mongoManager) ?: return@post;
                        PermissionHandler().checkIfRequestUserIdAdmin(call, user);
                        Accounts().AddRole(call, mongoManager);
                    }
                    post("/{id}/roles/remove") {
                        val user: Account = isSignedIn(call, mongoManager) ?: return@post;
                        PermissionHandler().checkIfRequestUserIdAdmin(call, user);
                        Accounts().RemoveRole(call, mongoManager);
                    }
                }

                get("/roles") {
                    val user: Account = isSignedIn(call, mongoManager) ?: return@get;
                    PermissionHandler().checkIfRequestUserIdAdmin(call, user);
                    Roles().GetAll(call, mongoManager);
                }
                route("/roles") {
                    put("/create") {
                        val user: Account = isSignedIn(call, mongoManager) ?: return@put;
                        PermissionHandler().checkIfRequestUserIdAdmin(call, user);
                        Roles().Create(call, mongoManager);
                    }
                    delete("/delete") {
                        val user: Account = isSignedIn(call, mongoManager) ?: return@delete;
                        PermissionHandler().checkIfRequestUserIdAdmin(call, user);
                        Roles().Delete(call, mongoManager);
                    }
                }

                get("/registration-codes") {
                    val user: Account = isSignedIn(call, mongoManager) ?: return@get;
                    PermissionHandler().checkIfRequestUserIdAdmin(call, user);
                    RegistrationCodes().GetAll(call, mongoManager);
                }
                route("/registration-codes") {
                    put("/create") {
                        val user: Account = isSignedIn(call, mongoManager) ?: return@put;
                        PermissionHandler().checkIfRequestUserIdAdmin(call, user);
                        RegistrationCodes().Create(call, mongoManager);
                    }
                    delete("/delete") {
                        val user: Account = isSignedIn(call, mongoManager) ?: return@delete;
                        PermissionHandler().checkIfRequestUserIdAdmin(call, user);
                        RegistrationCodes().Delete(call, mongoManager);
                    }
                }

                get("/settings") {
                    val user: Account = isSignedIn(call, mongoManager) ?: return@get;
                    PermissionHandler().checkIfRequestUserIdAdmin(call, user);
                    Settings().Get(call, mongoManager);
                }
                route("/settings") {
                    put("/update") {
                        val user: Account = isSignedIn(call, mongoManager) ?: return@put;
                        PermissionHandler().checkIfRequestUserIdAdmin(call, user);
                        Settings().Update(call, mongoManager);
                    }
                }
            }
        }
    }
}
