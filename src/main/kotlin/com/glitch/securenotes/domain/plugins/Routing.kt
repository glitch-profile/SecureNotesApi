package com.glitch.securenotes.domain.plugins

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun Application.configureRouting() {
    routing {

        staticFiles(
            remotePath = "/static",
            File("resources")
        )

    }
}
