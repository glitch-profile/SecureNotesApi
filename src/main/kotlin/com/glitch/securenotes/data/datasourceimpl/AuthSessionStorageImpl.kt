package com.glitch.securenotes.data.datasourceimpl

import com.glitch.securenotes.data.datasource.AuthSessionStorage
import com.glitch.securenotes.data.exceptions.auth.SessionNotFoundException
import com.glitch.securenotes.domain.sessions.AuthSession
import io.ktor.server.config.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import java.io.File
import java.nio.file.Paths

class AuthSessionStorageImpl: AuthSessionStorage {
    private val isPackedForExternal = ApplicationConfig(null).tryGetString("app.is_for_external").toBoolean()
    private val sessionsStorageDir = if (isPackedForExternal) File("${Paths.get("")}/sessions")
    else File("build/.sessions")

    private val authSecretKey = ApplicationConfig(null).tryGetString("app.security.auth_secret")!!
    private val secret = hex(authSecretKey)

    override val sessionStorage = directorySessionStorage(sessionsStorageDir)
    override val sessionSerializer = defaultSessionSerializer<AuthSession>()
    override val sessionIdEncryptor = SessionTransportTransformerMessageAuthentication(secret)

    override suspend fun isIdExists(id: String): Boolean {
        return sessionStorage.read(id).isNotEmpty()
    }

    override fun encryptSessionId(sessionId: String): String {
        return sessionIdEncryptor.transformWrite(sessionId)
    }

    override fun isEncryptionValid(encryptedSessionId: String): Boolean {
        val sessionId = encryptedSessionId.substringBefore('/')
        val decryptedSessionId = sessionIdEncryptor.transformRead(encryptedSessionId)
        return sessionId == decryptedSessionId
    }

    override suspend fun get(id: String): AuthSession {
        val sessionData = sessionStorage.read(id)
        if (sessionData.isEmpty()) throw SessionNotFoundException()
        return sessionSerializer.deserialize(sessionData)
    }

    override suspend fun delete(id: String) {
        sessionStorage.invalidate(id)
    }

    override suspend fun write(id: String, authData: AuthSession) {
        val serializedAuthData = sessionSerializer.serialize(authData)
        sessionStorage.write(
            id = id,
            value = serializedAuthData
        )
    }
}