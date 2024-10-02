package com.glitch.securenotes.data.datasource

import com.glitch.securenotes.domain.sessions.AuthSession
import io.ktor.server.sessions.*
import java.time.OffsetDateTime
import java.time.ZoneId

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
        sessionId: String
    ): AuthSession

    suspend fun delete(
        sessionId: String
    )

    suspend fun updateLastActivity(
        sessionId: String,
        lastActivityTimestamp: Long = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
    )

    suspend fun updateMaxSessionDuration(
        sessionId: String,
        maxDurationInHours: Int?
    )

    suspend fun createSession(
        sessionId: String,
        userId: String,
        platformName: String,
        appVersion: String,
        maxDurationInHours: Int?
    )
}