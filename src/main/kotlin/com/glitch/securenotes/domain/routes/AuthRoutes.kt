package com.glitch.securenotes.domain.routes

import com.glitch.securenotes.data.datasource.UserCredentialsDataSource
import com.glitch.securenotes.data.datasource.UsersDataSource
import com.glitch.securenotes.data.datasourceimpl.AuthSessionStorageImpl
import com.glitch.securenotes.data.exceptions.auth.CredentialsNotFoundException
import com.glitch.securenotes.data.exceptions.users.UserNotFoundException
import com.glitch.securenotes.data.model.dto.ApiResponseDto
import com.glitch.securenotes.data.model.dto.auth.AuthIncomingLoginData
import com.glitch.securenotes.domain.sessions.AuthSession
import com.glitch.securenotes.domain.utils.ApiErrorCode
import com.glitch.securenotes.domain.utils.codeauth.CodeAuthenticator
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import org.koin.ktor.ext.inject
import java.time.OffsetDateTime
import java.time.ZoneId

private const val PATH = "/api/V1/auth"

fun Route.authRoutes(
    userCredentialsDataSource: UserCredentialsDataSource,
    usersDataSource: UsersDataSource,
    codeAuthenticator: CodeAuthenticator
) {

    val authSessionsManager by inject<AuthSessionStorageImpl>()

    webSocket("$PATH/otp-socket") {
        val userId = codeAuthenticator.generateUserId()
        try {
            codeAuthenticator.joinRoom(userId = userId, webSocketConnection = this)
            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    try {
                        val command = frame.readText()
                        when (command) {
                            "update-code" -> {
                                val newCode = codeAuthenticator.generateUniqueCode()
                                codeAuthenticator.updateCode(
                                    userId = userId,
                                    newCode = newCode
                                )
                            }
                        }
                    } catch (e: Exception) {
                        println("AUTH ROUTES - CODE SOCKET - ERROR - ${e.stackTrace}")
                    }
                }
            }
        } catch (e: Exception) {
            e.message
            call.respond(HttpStatusCode.Conflict)
        } finally {
            codeAuthenticator.leaveRoom(userId)
        }
    }

    post("$PATH/guest") {
        try {
            val sessionId = generateSessionId()
            authSessionsManager.write(
                sessionId,
                AuthSession(
                    userId = "0",
                    expireTimestamp = null
                )
            )
            val encryptedSessionId = authSessionsManager.encryptSessionId(sessionId)
            call.respond(
                ApiResponseDto.Success(
                    data = encryptedSessionId
                )
            )
        } catch (e: Exception) {
            call.respond(
                ApiResponseDto.Error<Unit>(
                    apiErrorCode = ApiErrorCode.UNKNOWN_ERROR,
                    message = e.message ?: "Unable to create guest session"
                )
            )
        }
    }

    post("$PATH/login") {
        try {
            val authData = call.receiveNullable<AuthIncomingLoginData>() ?: kotlin.run {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            val userId = userCredentialsDataSource.auth(
                login = authData.login.take(15),
                password = authData.password.take(15)
            )
            val user = usersDataSource.getUserById(userId)
            // do some check here
            val sessionId = generateSessionId()
            authSessionsManager.write(
                id = sessionId,
                authData = AuthSession(
                    userId = user.id,
                    expireTimestamp = OffsetDateTime.now(ZoneId.systemDefault())
                        .plusMonths(6L)
                        .toEpochSecond()
                )
            )
            val encryptedSessionId = authSessionsManager.encryptSessionId(sessionId)
            call.respond(
                ApiResponseDto.Success(
                    data = encryptedSessionId
                )
            )
        } catch (e: CredentialsNotFoundException) {
            call.respond(
                ApiResponseDto.Error<Unit>(
                    apiErrorCode = ApiErrorCode.AUTH_DATA_INCORRECT,
                    message = "Credentials not found"
                )
            )
        } catch (e: UserNotFoundException) {
            e.printStackTrace()
            call.respond(
                ApiResponseDto.Error<Unit>(
                    apiErrorCode = ApiErrorCode.USER_NOT_FOUND,
                    message = "User not found"
                )
            )
        }
    }

    authenticate("guest") {
        get("$PATH/test") {
            val session = call.sessions.get<AuthSession>() ?: kotlin.run {
                call.respond("No session provided")
                return@get
            }
            call.respond("${session.userId} - ${session.expireTimestamp}")
        }
    }

    authenticate("user") {
        post("$PATH/confirm-code") {

        }
    }

}