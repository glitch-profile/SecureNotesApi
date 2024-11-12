package com.glitch.securenotes.domain.plugins

import com.glitch.securenotes.domain.sessions.AuthSession
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*

fun Application.configureAuthentication() {

    install(Authentication) {
        session<AuthSession>(AuthenticationLevel.GUEST) {
            validate { session ->
                if (session.userId == "0") session else null
            }
            challenge {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
        session<AuthSession>(AuthenticationLevel.USER) {
            validate { session ->
                if (session.userId != "0") session else null
            }
            challenge {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }

}

object AuthenticationLevel {
    const val GUEST = "guest"
    const val USER = "user"
}
