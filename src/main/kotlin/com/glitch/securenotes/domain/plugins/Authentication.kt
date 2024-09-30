package com.glitch.securenotes.domain.plugins

import com.glitch.securenotes.domain.sessions.AuthSession
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*

fun Application.configureAuthentication() {

    install(Authentication) {
        session<AuthSession>("guest") {
            validate { session ->
                if (session.userId == "0") session else null
            }
            challenge {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
        session<AuthSession>("user") {
            validate { session ->
                if (session.userId != "0") session else null
            }
            challenge {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }

}