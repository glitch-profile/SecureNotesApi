package com.glitch.securenotes.domain.rooms.noteslist

import kotlinx.serialization.Serializable

@Serializable
data class NotesListSocketEvent(
    val initiatedUser: String,
    val eventType: Short,
    val affectedNoteIds: List<String>
) {
    companion object {
        const val ADDED_NEW_NOTE: Short = 1
        const val UPDATED_NOTE: Short = 2
        const val UPDATED_NOTE_PREVIEW: Short = 3
        const val DELETED_NOTE: Short = 4
        const val UPDATED_ROLE_TO_EDITOR: Short = 5
        const val UPDATED_ROLE_TO_READER: Short = 6
        const val UPDATED_ROLE_TO_OWNER: Short = 7
    }
}
