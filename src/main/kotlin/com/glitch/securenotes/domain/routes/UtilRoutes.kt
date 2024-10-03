package com.glitch.securenotes.domain.routes

import com.glitch.securenotes.data.model.dto.utils.PingInfoDto
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.utilRoutes() {

    route("api/V1/utils") {

        route("/ping") {

            get {
//                val clientAddress = call.request.origin.remoteHost
                val clientAddress = call.request.origin.remoteAddress
                val hostAddress = call.request.origin.localAddress
                val currentServerTimeMillis = System.currentTimeMillis()
                call.respond(
                    PingInfoDto(
                        currentServerMillis = currentServerTimeMillis,
                        hostAddress = hostAddress,
                        remoteAddress = clientAddress
                    )
                )
            }

            // Minimal ping request
            get("/m") {
                call.respondText("Ping!")
            }

        }

    }

}