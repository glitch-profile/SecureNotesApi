package com.glitch.securenotes.domain.rooms.noteslist

import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

// Manages information about notes available to users using WebSocketSessions
class UserNotesRoomControllerImpl: UserNotesRoomController {

    private val rooms = ConcurrentHashMap<String, NotesListUserInfo>()

    private val jsonSerializer = Json {
        encodeDefaults = true
        isLenient = true
    }

    override fun isUserInRoom(userId: String): Boolean {
        return rooms.containsKey(userId)
    }

    override fun joinRoom(userId: String, userProtectedNotes: Set<String>, webSocketSession: WebSocketSession) {
        val userInfo = NotesListUserInfo(
            userId = userId,
            protectedNoteIds = userProtectedNotes,
            socketSession = webSocketSession
        )
        rooms[userId] = userInfo
    }

    override suspend fun sendEventForUser(userId: String, event: NotesListSocketEvent) {
        val userToUpdate = rooms.values.firstOrNull {
            userId == it.userId && (it.isUpdatingProtectedNotes || !it.protectedNoteIds.contains(event.affectedNoteId))
        }
        if (userToUpdate != null) {
            val serializedEvent = jsonSerializer.encodeToString(event)
            userToUpdate.socketSession.send(serializedEvent)
        }
    }

    override suspend fun sendEventForUsers(userIds: List<String>, event: NotesListSocketEvent) {
        val usersToUpdate = rooms.filterValues {
            userIds.contains(it.userId)
                    && (it.isUpdatingProtectedNotes || !it.protectedNoteIds.contains(event.affectedNoteId))
        }
        val serializedEvent = jsonSerializer.encodeToString(event)
        usersToUpdate.forEach { it.value.socketSession.send(serializedEvent) }
    }

    override fun enableUpdatesForSecuredNotes(userId: String) {
        if (isUserInRoom(userId)) {
            val userInfo = rooms[userId]!!
            rooms[userId] = userInfo.copy(
                isUpdatingProtectedNotes = true
            )
        }
    }

    override fun disableUpdatesForSecuredNotes(userId: String) {
        if (isUserInRoom(userId)) {
            val userInfo = rooms[userId]!!
            rooms[userId] = userInfo.copy(
                isUpdatingProtectedNotes = false
            )
        }
    }

    override fun addNotesToProtected(userId: String, noteIds: Set<String>) {
        if (isUserInRoom(userId)) {
            val userInfo = rooms[userId]!!
            rooms[userId] = userInfo.copy(
                protectedNoteIds = userInfo.protectedNoteIds.toMutableSet().apply {
                    addAll(noteIds)
                }
            )
        }
    }

    override fun removeNotesFromProtected(userId: String, noteIds: Set<String>) {
        if (isUserInRoom(userId)) {
            val userInfo = rooms[userId]!!
            rooms[userId] = userInfo.copy(
                protectedNoteIds = userInfo.protectedNoteIds.toMutableSet().apply {
                    removeAll(noteIds)
                }
            )
        }
    }

    override fun leaveRoom(userId: String) {
        rooms.remove(userId)
    }
}