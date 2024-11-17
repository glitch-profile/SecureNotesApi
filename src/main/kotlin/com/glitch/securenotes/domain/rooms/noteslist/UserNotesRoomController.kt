package com.glitch.securenotes.domain.rooms.noteslist

import io.ktor.websocket.*

interface UserNotesRoomController {

    fun isUserInRoom(userId: String): Boolean

    fun joinRoom(
        userId: String,
        userProtectedNotes: Set<String>,
        webSocketSession: WebSocketSession
    )

    suspend fun sendEventForUser(
        userId: String,
        event: NotesListSocketEvent
    )

    suspend fun sendEventForUsers(
        userIds: List<String>,
        event: NotesListSocketEvent
    )

    fun enableUpdatesForSecuredNotes(userId: String)

    fun disableUpdatesForSecuredNotes(userId: String)

    fun addNotesToProtected(userId: String, noteIds: Set<String>)

    fun removeNotesFromProtected(userId: String, noteIds: Set<String>)

    fun leaveRoom(userId: String)

}