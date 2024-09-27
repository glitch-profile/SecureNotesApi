package com.glitch.securenotes.domain.utils.codeauth

import io.ktor.websocket.*

interface CodeAuthenticator {

    fun generateUserId(): String

    fun isUserIdExists(userId: String): Boolean

    fun generateUniqueCode(): String

    fun isCodeExists(code: String): Boolean

    suspend fun joinRoom(
        userId: String,
        webSocketConnection: WebSocketSession
    )

    suspend fun leaveRoom(
        userId: String
    )

    suspend fun confirmCode(
        code: String,
        sessionIdToAssign: String
    )

    suspend fun updateCode(
        userId: String,
        newCode: String
    )

    suspend fun onConnectionExpire(
        userId: String
    )

}