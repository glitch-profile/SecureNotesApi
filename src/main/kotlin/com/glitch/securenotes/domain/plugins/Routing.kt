package com.glitch.securenotes.domain.plugins

import com.glitch.floweryapi.domain.utils.encryptor.AESEncryptor
import com.glitch.securenotes.data.datasource.UserCredentialsDataSource
import com.glitch.securenotes.data.datasource.UsersDataSource
import com.glitch.securenotes.domain.routes.authRoutes
import com.glitch.securenotes.domain.utils.codeauth.CodeAuthenticator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.delay
import org.koin.ktor.ext.inject
import java.io.File

fun Application.configureRouting() {
    val userCredentialsDataSource by inject<UserCredentialsDataSource>()
    val usersDataSource by inject<UsersDataSource>()
    val codeAuthenticator by inject<CodeAuthenticator>()

    routing {

        authRoutes(
            userCredentialsDataSource, usersDataSource, codeAuthenticator
        )

        // should use this instead of static resources
        get("test") {
            val file = File("D:/test/123.png")
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.FileName, "test.jpg").toString()
            )
            val byteArray = file.inputStream().use {
                it.readBytes()
            }
            call.respondBytes(byteArray)
        }
        get("test2") {
            val hasAuthority = call.request.header("user_id") == "12345"
            if (!hasAuthority) call.respond(HttpStatusCode.Forbidden)
            val file = File("D:/test/123.jpg")
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
        get("files/{filePath...}") {
            val imagePath = call.pathParameters.getAll("filePath")!!.joinToString("/")
            call.respondText(imagePath)
        }
    }
}
