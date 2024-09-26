package com.glitch.securenotes.domain.plugins

import com.glitch.securenotes.data.datasource.AuthSessionStorage
import com.glitch.securenotes.domain.sessions.AuthSession
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import org.koin.ktor.ext.inject

fun Application.configureSessions() {
    val authSecretKey = ApplicationConfig(null).tryGetString("app.security.auth_secret")!!
    val authSessionStorage by inject<AuthSessionStorage>()

    install(Sessions) {
        val secret = hex(authSecretKey)

        header<AuthSession>(
            name = "auth_session",
            storage = authSessionStorage.sessionStorage
        ) {
            transform(SessionTransportTransformerMessageAuthentication(secret))
        }
    }
}
