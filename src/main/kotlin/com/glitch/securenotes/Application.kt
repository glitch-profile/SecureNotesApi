package com.glitch.securenotes

import com.glitch.securenotes.domain.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureKoin()
    configureSessions()
    configureAuthentication()
    configureSerialization()
    configureWebSockets()
    configureRouting()
}
