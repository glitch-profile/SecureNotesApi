package com.glitch.securenotes.domain.rooms.noteslist

import io.ktor.websocket.*

data class NotesListUserInfo(
    val userId: String,
    val protectedNoteIds: Set<String>,
    val socketSession: WebSocketSession,
    val isUpdatingProtectedNotes: Boolean = false

)
