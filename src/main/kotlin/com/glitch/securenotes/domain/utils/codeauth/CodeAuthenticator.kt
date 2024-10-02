package com.glitch.securenotes.domain.utils.codeauth

import io.ktor.websocket.*

interface CodeAuthenticator {

    fun generateUserId(): String

    fun isUserIdExists(userId: String): Boolean

    fun generateUniqueCode(): String

    fun isCodeExists(code: String): Boolean

    suspend fun joinRoom(
        userId: String,
        webSocketConnection: WebSocketSession,
        platformString: String,
        appVersionString: String
    )

    suspend fun leaveRoom(
        userId: String
    )

    suspend fun getAuthMemberForCode(
        code: String
    ): CodeAuthMember

    suspend fun confirmCode(
        code: String,
        sessionId: String
    )

    suspend fun updateCode(
        userId: String,
        newCode: String
    )

    suspend fun onConnectionExpire(
        userId: String
    )

}