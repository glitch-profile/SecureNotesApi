package com.glitch.securenotes.domain.utils.codeauth

import com.glitch.securenotes.data.model.dto.auth.AuthSocketEventData
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

private const val CODE_DURATION_DEFAULT = 600_000L // 10 minutes in millis

class CodeAuthenticatorImpl: CodeAuthenticator {

    private val connection = ConcurrentHashMap<String, WebSocketSession>()
    private val connectionExpireJobs = ConcurrentHashMap<String, Job>()
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val jsonSerializer = Json {
        encodeDefaults = true
        isLenient = true
    }

    override fun generateRandomCode(): String {
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
        } while (connection.containsKey(code))
        return code
    }

    override fun isCodeExists(code: String): Boolean {
        return connection.containsKey(code)
    }

    override suspend fun joinRoom(code: String, webSocketConnection: WebSocketSession) {
        if (isCodeExists(code)) throw CodeAlreadyExistsException()
        connection[code] = webSocketConnection
        connectionExpireJobs[code] = expireConnectionJob(code)
        val eventData = AuthSocketEventData(
            eventCode = CodeAuthEvent.CODE_GENERATED,
            data = code
        )
        webSocketConnection.send(
            Frame.Text(jsonSerializer.encodeToString(eventData))
        )
    }

    override suspend fun leaveRoom(code: String) {
        if (!isCodeExists(code)) throw CodeNotFoundException()
        connection[code]?.close()
        connection.remove(code)
        connectionExpireJobs.remove(code)
    }

    override suspend fun confirmCode(code: String, sessionIdToAssign: String) {
        if (!isCodeExists(code)) throw CodeNotFoundException()
        connection[code]?.let { socketSession ->
            val eventData = AuthSocketEventData(
                eventCode = CodeAuthEvent.CODE_CONFIRMED,
                data = sessionIdToAssign
            )
            val encodedMessage = jsonSerializer.encodeToString(eventData)
            socketSession.send(
                Frame.Text(encodedMessage)
            )
            leaveRoom(code)
        }
    }

    override suspend fun updateCode(oldCode: String, newCode: String) {
        if (!isCodeExists(oldCode)) throw CodeNotFoundException()
        if (isCodeExists(newCode)) throw CodeAlreadyExistsException()
        val socketSession = connection[oldCode]!!
        connection.remove(oldCode)
        connection[newCode] = socketSession
        val eventData = AuthSocketEventData(
            eventCode = CodeAuthEvent.CODE_UPDATED,
            data = newCode
        )
        socketSession.send(
            Frame.Text(jsonSerializer.encodeToString(eventData))
        )
    }

    override suspend fun closeConnection(code: String) {
        if (!isCodeExists(code)) throw CodeNotFoundException()
        connection[code]?.let { socketSession ->
            val eventData = AuthSocketEventData(
                eventCode = CodeAuthEvent.CONNECTION_EXPIRED,
                data = null
            )
            socketSession.send(
                Frame.Text(
                    jsonSerializer.encodeToString(eventData)
                )
            )
            leaveRoom(code)
        }
    }

    private fun expireConnectionJob(
        code: String,
        expireDelayMillis: Long = CODE_DURATION_DEFAULT
    ): Job {
        return coroutineScope.launch {
            delay(expireDelayMillis)
            closeConnection(code)
        }
    }
}