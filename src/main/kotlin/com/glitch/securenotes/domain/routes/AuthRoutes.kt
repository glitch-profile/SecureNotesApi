package com.glitch.securenotes.domain.routes

import com.glitch.securenotes.data.datasource.AuthSessionStorage
import com.glitch.securenotes.data.datasource.UserCredentialsDataSource
import com.glitch.securenotes.data.datasource.UsersDataSource
import com.glitch.securenotes.data.exceptions.auth.CredentialsNotFoundException
import com.glitch.securenotes.data.exceptions.auth.LoginAlreadyInUseException
import com.glitch.securenotes.data.exceptions.users.UserNotFoundException
import com.glitch.securenotes.data.model.dto.ApiResponseDto
import com.glitch.securenotes.data.model.dto.auth.AuthIncomingCodeConfirmationDto
import com.glitch.securenotes.data.model.dto.auth.AuthIncomingLoginDto
import com.glitch.securenotes.data.model.dto.auth.AuthIncomingNewAccountDto
import com.glitch.securenotes.data.model.dto.auth.AuthOutgoingInfoDto
import com.glitch.securenotes.domain.sessions.AuthSession
import com.glitch.securenotes.domain.utils.ApiErrorCode
import com.glitch.securenotes.domain.utils.codeauth.CodeAuthenticator
import com.glitch.securenotes.domain.utils.codeauth.CodeNotFoundException
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach

fun Route.authRoutes(
    userCredentialsDataSource: UserCredentialsDataSource,
    usersDataSource: UsersDataSource,
    codeAuthenticator: CodeAuthenticator,
    authSessionsManager: AuthSessionStorage
) {

    route("api/V1/auth") {

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

        webSocket(
            path = "/otp-socket"
        ) {
            val platform = call.request.queryParameters["platform"]
            val appAgent = call.request.queryParameters["agent"]
            if (platform == null || appAgent == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@webSocket
            }
            println(appAgent)
            val userId = codeAuthenticator.generateUserId()
            try {
                codeAuthenticator.joinRoom(
                    userId = userId,
                    webSocketConnection = this,
                    platformString = platform,
                    appVersionString = appAgent
                )
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
                call.respond(HttpStatusCode.Conflict)
            } finally {
                codeAuthenticator.leaveRoom(userId)
            }
        }

        post("/guest") {
            try {
                val platform = call.request.queryParameters["platform"] ?: kotlin.run {
                    call.request.header("sec-ch-ua-platform")
                }
                val agent = call.request.queryParameters["agent"] ?: kotlin.run {
                    call.request.header("sec-ch-ua")
                }
                if (platform == null || agent == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }

                val formattedPlatformName = platform.filterNot { it == '"' }
                val formattedAgent = getUserBrowserData(agent)
                val sessionId = generateSessionId()
                authSessionsManager.createSession(
                    sessionId = sessionId,
                    userId = "0",
                    platformName = formattedPlatformName,
                    appVersion = formattedAgent,
                    maxDurationInHours = null
                )
                val encryptedSessionId = authSessionsManager.encryptSessionId(sessionId)
                call.respond(
                    ApiResponseDto.Success(
                        data = AuthOutgoingInfoDto(
                            sessionId = encryptedSessionId,
                            userId = "0"
                        )
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

        post("/login") {
            try {
                val authData = call.receiveNullable<AuthIncomingLoginDto>() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
                val platform = call.request.queryParameters["platform"] ?: kotlin.run {
                    call.request.header("sec-ch-ua-platform")
                }
                val agent = call.request.queryParameters["agent"] ?: kotlin.run {
                    call.request.header("sec-ch-ua")
                }
                if (platform == null || agent == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }

                val formattedPlatformName = platform.filterNot { it == '"' }
                val formattedAgent = getUserBrowserData(agent)
                val userId = userCredentialsDataSource.auth(
                    login = authData.login.take(20),
                    password = authData.password.take(20)
                )
                val user = usersDataSource.getOneUserById(userId)
                // do some check here
                val sessionId = generateSessionId()
                authSessionsManager.createSession(
                    sessionId = sessionId,
                    userId = user.id,
                    platformName = formattedPlatformName,
                    appVersion = formattedAgent,
                    maxDurationInHours = null
                )
                usersDataSource.addActiveSessionId(
                    userId = user.id,
                    sessionId = sessionId
                )
                val encryptedSessionId = authSessionsManager.encryptSessionId(sessionId)
                call.respond(
                    ApiResponseDto.Success(
                        data = AuthOutgoingInfoDto(
                            sessionId = encryptedSessionId,
                            userId = user.id
                        )
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

        post("/signup") {
            val newAccountData = call.receiveNullable<AuthIncomingNewAccountDto>() ?: kotlin.run {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            val platform = call.request.queryParameters["platform"] ?: kotlin.run {
                call.request.header("sec-ch-ua-platform")
            }
            val agent = call.request.queryParameters["agent"] ?: kotlin.run {
                call.request.header("sec-ch-ua")
            }
            if (platform == null || agent == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            val formattedPlatformName = platform.filterNot { it == '"' }
            val formattedAgent = getUserBrowserData(agent)
            val authDataFormatted = newAccountData.copy(
                username = newAccountData.username.filter {
                    it.isLetterOrDigit() || it in "~_-+=*#@!<>,./?"
                }.take(20),
                login = newAccountData.login.take(20).filterNot {
                    it.isISOControl() || it.isWhitespace()
                }.take(20),
                password = newAccountData.password.filterNot {
                    it.isISOControl() || it.isWhitespace()
                }.take(20)
            )
            val newUserModel = usersDataSource.addUser(authDataFormatted.login)
            try {
                userCredentialsDataSource.addCredentials(
                    userId = newUserModel.id,
                    login = authDataFormatted.login,
                    password = authDataFormatted.password
                )
                val sessionId = generateSessionId()
                authSessionsManager.createSession(
                    sessionId = sessionId,
                    userId = newUserModel.id,
                    platformName = formattedPlatformName,
                    appVersion = formattedAgent,
                    maxDurationInHours = null

                )
                val encryptedSessionId = authSessionsManager.encryptSessionId(sessionId)
                call.respond(
                    ApiResponseDto.Success(
                        data = AuthOutgoingInfoDto(
                            sessionId = encryptedSessionId,
                            userId = newUserModel.id
                        )
                    )
                )
            } catch (e: LoginAlreadyInUseException) {
                kotlin.runCatching { usersDataSource.deleteUserById(newUserModel.id) }
                call.respond(
                    ApiResponseDto.Error<Unit>(
                        apiErrorCode = ApiErrorCode.CREDENTIALS_ALREADY_IN_USE,
                        message = "Credentials already in use"
                    )
                )
            }
        }

        authenticate("user") {

            post("/confirm-code") {
                try {
                    val codeAuthData = call.receiveNullable<AuthIncomingCodeConfirmationDto>() ?: kotlin.run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }
                    val session = call.sessions.get<AuthSession>()!!
                    val userInfo = usersDataSource.getOneUserById(session.userId)
                    val newSessionId = generateSessionId()
                    val codeMemberAuthData = codeAuthenticator.getAuthMemberForCode(codeAuthData.code)
                    authSessionsManager.createSession(
                        sessionId = newSessionId,
                        userId = userInfo.id,
                        platformName = codeMemberAuthData.platform,
                        appVersion = codeMemberAuthData.appVersion,
                        maxDurationInHours = codeAuthData.maxDurationHours
                    )
                    codeAuthenticator.confirmCode(
                        code = codeAuthData.code,
                        sessionId = newSessionId
                    )
                    call.respond(
                        ApiResponseDto.Success(null)
                    )
                } catch (e: UserNotFoundException) {
                    call.respond(
                        ApiResponseDto.Error<Unit>(
                            apiErrorCode = ApiErrorCode.USER_NOT_FOUND,
                            message = "User with this id not found"
                        )
                    )
                } catch (e: CodeNotFoundException) {
                    call.respond(
                        ApiResponseDto.Error<Unit>(
                            apiErrorCode = ApiErrorCode.AUTH_CODE_NOT_FOUND,
                            message = "This code not found or expired"
                        )
                    )
                }
            }

            get("/auth-info") {
                val session = call.sessions.get<AuthSession>()!!
                val sessionId = call.sessionId<AuthSession>()!!
                call.respond(
                    ApiResponseDto.Success(
                        data = AuthOutgoingInfoDto(
                            sessionId = sessionId,
                            userId = session.userId
                        )
                    )
                )
            }

        }

    }

}