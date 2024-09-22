package com.glitch.securenotes.domain.plugins

import com.glitch.securenotes.domain.sessions.AuthSession
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import java.io.File
import java.nio.file.Paths

fun Application.configureSessions() {
    val authSecretKey = ApplicationConfig(null).tryGetString("app.security.auth_secret")!!
    val isPackedForExternal = ApplicationConfig(null).tryGetString("app.is_for_external").toBoolean()

    install(Sessions) {
        val secret = hex(authSecretKey)

        header<AuthSession>(
            name = "auth_session",
            storage = if (isPackedForExternal) directorySessionStorage(File("${Paths.get("")}/sessions"))
                else directorySessionStorage(File("build/.sessions"))
        ) {
            transform(SessionTransportTransformerMessageAuthentication(secret))
        }
    }
}
