package com.glitch.securenotes.data.datasource

import com.glitch.securenotes.domain.sessions.AuthSession
import io.ktor.server.sessions.*

interface AuthSessionStorage {
    val sessionStorage: SessionStorage
    val sessionSerializer: SessionSerializer<AuthSession>
    val sessionIdEncryptor: SessionTransportTransformerMessageAuthentication

    suspend fun isIdExists(id: String): Boolean

    fun encryptSessionId(
        sessionId: String
    ): String

    fun isEncryptionValid(
        encryptedSessionId: String
    ): Boolean

    suspend fun get(
        id: String
    ): AuthSession

    suspend fun delete(
        id: String
    )

    suspend fun write(
        id: String,
        authData: AuthSession
    )
}