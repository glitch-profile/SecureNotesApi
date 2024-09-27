package com.glitch.securenotes.domain.utils.codeauth

import com.glitch.securenotes.data.model.dto.auth.AuthSocketEventData
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

private const val CODE_DURATION_DEFAULT = 600_000L // 10 minutes in millis

class CodeAuthenticatorImpl: CodeAuthenticator {

    private val connections = ConcurrentHashMap<String, CodeAuthMember>()
    private val connectionExpireJobs = ConcurrentHashMap<String, Job>()
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val jsonSerializer = Json {
        encodeDefaults = true
        isLenient = true
    }

    override fun generateUniqueCode(): String {
        val availableChars = "0123456789"
        var code: String
        var totalTries = 0
        do {
            code = ""
            totalTries += 1
            val codeLength = 4 + ( totalTries / 20 )
            repeat(codeLength) {
                val randomChar = availableChars[Random.nextInt(0, availableChars.length)]
                code += randomChar
            }
        } while (!isCodeExists(code))
        return code
    }

    override fun isCodeExists(code: String): Boolean {
        return connections.none { it.value.code == code }
    }

    override fun generateUserId(): String {
        var userId: String
        do {
            userId = UUID.randomUUID().toString()
        } while (!isUserIdExists(userId))
        return userId
    }

    override fun isUserIdExists(userId: String): Boolean {
        return connections.containsKey(userId)
    }

    override suspend fun joinRoom(userId: String, webSocketConnection: WebSocketSession) {
        if (isUserIdExists(userId)) throw UserAlreadyExistsException()
        val code = generateUniqueCode()
        connections[userId] = CodeAuthMember(
            code = code,
            socketSession = webSocketConnection
        )
        connectionExpireJobs[userId] = expireConnectionJob(userId)
        val codeCreatedEvent = AuthSocketEventData(
            eventCode = CodeAuthEvent.CODE_GENERATED,
            data = code
        )
        webSocketConnection.send(
            Frame.Text(
                jsonSerializer.encodeToString(codeCreatedEvent)
            )
        )
    }

    override suspend fun leaveRoom(userId: String) {
        connections[userId]?.socketSession?.close()
        connections.remove(userId)
        connectionExpireJobs[userId]?.cancel()
        connectionExpireJobs.remove(userId)
    }

    override suspend fun onConnectionExpire(userId: String) {
        connections[userId]?.let { codeAuthMember ->
            val socketSession = codeAuthMember.socketSession
            val connectionExpireEvent = AuthSocketEventData(
                eventCode = CodeAuthEvent.CONNECTION_EXPIRED,
                data = null
            )
            socketSession.send(
                Frame.Text(
                    jsonSerializer.encodeToString(connectionExpireEvent)
                )
            )
            leaveRoom(userId)
        }
    }

    override suspend fun confirmCode(code: String, sessionIdToAssign: String) {
        val codeAuthMember = connections.filterValues { it.code == code }.entries.firstOrNull() ?: kotlin.run {
            throw CodeNotFoundException()
        }
        val socketSession = codeAuthMember.value.socketSession
        val codeConfirmEvent = AuthSocketEventData(
            eventCode = CodeAuthEvent.CODE_CONFIRMED,
            data = sessionIdToAssign
        )
        socketSession.send(
            Frame.Text(jsonSerializer.encodeToString(codeConfirmEvent))
        )
        leaveRoom(codeAuthMember.key)
    }

    override suspend fun updateCode(userId: String, newCode: String) {
        connections[userId]?.let { codeAuthMember ->
            connections[userId] = codeAuthMember.copy(code = newCode)
            val codeUpdatedEvent = AuthSocketEventData(
                eventCode = CodeAuthEvent.CODE_UPDATED,
                data = newCode
            )
            codeAuthMember.socketSession.send(
                Frame.Text(jsonSerializer.encodeToString(codeUpdatedEvent))
            )
        }
    }

    private fun expireConnectionJob(
        userId: String,
        expireDelayMillis: Long = CODE_DURATION_DEFAULT
    ): Job {
        return coroutineScope.launch {
            delay(expireDelayMillis)
            onConnectionExpire(userId)
        }
    }
}