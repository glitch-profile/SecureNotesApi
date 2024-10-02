package com.glitch.securenotes.domain.plugins

import com.glitch.floweryapi.domain.utils.encryptor.AESEncryptor
import com.glitch.securenotes.data.datasource.AuthSessionStorage
import com.glitch.securenotes.data.datasource.UserCredentialsDataSource
import com.glitch.securenotes.data.datasource.UsersDataSource
import com.glitch.securenotes.domain.routes.authRoutes
import com.glitch.securenotes.domain.utils.codeauth.CodeAuthenticator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.io.File

fun Application.configureRouting() {
    val userCredentialsDataSource by inject<UserCredentialsDataSource>()
    val usersDataSource by inject<UsersDataSource>()
    val codeAuthenticator by inject<CodeAuthenticator>()
    val authSessionManager by inject<AuthSessionStorage>()

    routing {

        fun getUserBrowserData(rawBrowserData: String): String {
            val rawBrowserDataExtractorRegex = Regex("[\"]([^\"]+)[\"'];v=[\"](\\d\\w+)[\"]")
            val lastIndexOfFirstPath = rawBrowserData.indexOfFirst { it == ',' }
            return if (lastIndexOfFirstPath == -1) {
                if (rawBrowserDataExtractorRegex.matches(rawBrowserData)) {
                    val findResult = rawBrowserDataExtractorRegex.find(rawBrowserData)
                    "${findResult!!.groups[1]!!.value} ${findResult.groups[2]!!.value}"
                } else rawBrowserData
            } else {
                val trimmedBrowserData = rawBrowserData.take(lastIndexOfFirstPath)
                val findResult = rawBrowserDataExtractorRegex.find(trimmedBrowserData)
                "${findResult!!.groups[1]!!.value} ${findResult.groups[2]!!.value}"
            }
        }

        authRoutes(
            userCredentialsDataSource,
            usersDataSource,
            codeAuthenticator,
            authSessionManager
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
            val platformName = call.request.header("sec-ch-ua-platform") ?: kotlin.run {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }
            val userBrowser = call.request.header("sec-ch-ua") ?: kotlin.run {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            val formattedPlatformName = platformName.filterNot { it == '"' }
            val formattedUserBrowser = getUserBrowserData(userBrowser)
            println(platformName)
            println(formattedUserBrowser)
        }
    }
}
