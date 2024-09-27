package com.glitch.securenotes.domain.utils.codeauth

import io.ktor.websocket.*

interface CodeAuthenticator {

    fun generateRandomCode(): String

    fun isCodeExists(code: String): Boolean

    suspend fun joinRoom(
        code: String,
        webSocketConnection: WebSocketSession
    )

    suspend fun leaveRoom(
        code: String
    )

    suspend fun confirmCode(
        code: String,
        sessionIdToAssign: String
    )

    suspend fun updateCode(
        oldCode: String,
        newCode: String
    )

    suspend fun closeConnection(
        code: String
    )

}