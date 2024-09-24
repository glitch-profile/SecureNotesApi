package com.glitch.securenotes.domain.plugins

import com.glitch.floweryapi.domain.utils.encryptor.AESEncryptor
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun Application.configureRouting() {
    routing {

        // should use this instead of static resources
        get("test") {
            val file = File("F:/test/123.png")
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.FileName, "1234.png").toString()
            )
            call.respondFile(file)
        }
        get("test2") {
            val hasAuthority = call.request.header("user_id") == "12345"
            if (!hasAuthority) call.respond(HttpStatusCode.Forbidden)
            val file = File("F:/test/123.png")
            val byteArray = file.inputStream().use {
                it.readBytes()
            }
            call.response.header(
                HttpHeaders.ContentType,
                ContentType.defaultForFile(file).toString()
            )
            call.respondBytes(byteArray)
        }
        get("test3") {
            val file = File("F:/test/test-e.txt")
            val byteArray = file.inputStream().use {
                it.readBytes()
            }
            val decryptedBytes = AESEncryptor.decrypt(byteArray)
            call.response.header(
                HttpHeaders.ContentType,
                ContentType.defaultForFile(file).toString()
            )
            call.respondBytes(decryptedBytes)
        }

    }
}
