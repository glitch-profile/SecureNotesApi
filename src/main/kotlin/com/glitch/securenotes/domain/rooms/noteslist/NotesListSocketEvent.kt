package com.glitch.securenotes.domain.rooms.noteslist

import com.glitch.securenotes.data.model.entity.NoteModel
import kotlinx.serialization.Serializable

//@Serializable
//data class NotesListSocketEvent(
//    val initiatedUser: String,
//    val eventType: Short,
//    val affectedNoteIds: List<String>
//) {

//}

@Serializable
sealed class NotesListSocketEvent {
    abstract val initiatedUserId: String
    abstract val eventCode: Short
    abstract val affectedNoteIds: List<String>?
    abstract val newRoleCode: Short?
    abstract val affectedNoteModel: NoteModel?

    @Serializable
    data class NewNote(
        override val initiatedUserId: String,
        override val affectedNoteModel: NoteModel
    ): NotesListSocketEvent() {
        override val eventCode = EVENT_ADDED_NEW_NOTE
        override val affectedNoteIds = null
        override val newRoleCode = null
    }

    @Serializable
    data class UpdatedNote(
        override val initiatedUserId: String,
        override val affectedNoteModel: NoteModel?
    ): NotesListSocketEvent() {
        override val eventCode = EVENT_UPDATED_NOTE
        override val affectedNoteIds = null
        override val newRoleCode = null
    }

    @Serializable
    data class DeletedNote(
        override val initiatedUserId: String,
        override val affectedNoteIds: List<String>
    ): NotesListSocketEvent() {
        override val eventCode = EVENT_DELETED_NOTE
        override val newRoleCode = null
        override val affectedNoteModel = null
    }

    @Serializable
    data class UpdatedRole(
        override val initiatedUserId: String,
        val affectedNoteId: String,
        override val newRoleCode: Short
    ): NotesListSocketEvent() {
        override val eventCode = EVENT_UPDATED_ROLE
        override val affectedNoteIds = listOf(affectedNoteId)
        override val affectedNoteModel = null
    }

    companion object {
        private const val EVENT_ADDED_NEW_NOTE: Short = 1
        private const val EVENT_UPDATED_NOTE: Short = 2
        private const val EVENT_DELETED_NOTE: Short = 3
        private const val EVENT_UPDATED_ROLE: Short = 4

        const val ROLE_OWNER: Short = 1
        const val ROLE_EDITOR: Short = 2
        const val ROLE_READER: Short = 3
    }
}
