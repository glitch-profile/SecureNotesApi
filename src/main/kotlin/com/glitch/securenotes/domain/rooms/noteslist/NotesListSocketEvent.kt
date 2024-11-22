package com.glitch.securenotes.domain.rooms.noteslist

import com.glitch.securenotes.data.model.dto.notes.NoteCompactInfoDto
import com.glitch.securenotes.data.model.dto.notes.NoteCompactUpdateInfoDto
import kotlinx.serialization.Serializable

//@Serializable
//data class NotesListSocketEvent(
//    val initiatedUser: String,
//    val eventType: Short,
//    val affectedNoteIds: List<String>
//) {

//}

private const val EVENT_ADDED_NEW_NOTE: Short = 1
private const val EVENT_UPDATED_NOTE: Short = 2
private const val EVENT_DELETED_NOTE: Short = 3
private const val EVENT_UPDATED_ROLE: Short = 4

@Serializable
sealed class NotesListSocketEvent {
    abstract val initiatedUserId: String
    abstract val eventCode: Short
    abstract val affectedNoteId: String
    abstract val newRoleCode: Short?
    abstract val newNoteInfoModel: NoteCompactInfoDto?
    abstract val updateNoteInfoModel: NoteCompactUpdateInfoDto?

    @Serializable
    data class NewNote(
        override val initiatedUserId: String,
        override val newNoteInfoModel: NoteCompactInfoDto
    ): NotesListSocketEvent() {
        override val eventCode = EVENT_ADDED_NEW_NOTE
        override val affectedNoteId = newNoteInfoModel.id
        override val updateNoteInfoModel: NoteCompactUpdateInfoDto? = null
        override val newRoleCode = null
    }

    @Serializable
    data class UpdatedNote(
        override val initiatedUserId: String,
        override val updateNoteInfoModel: NoteCompactUpdateInfoDto
    ): NotesListSocketEvent() {
        override val eventCode = EVENT_UPDATED_NOTE
        override val affectedNoteId = updateNoteInfoModel.id
        override val newNoteInfoModel: NoteCompactInfoDto? = null
        override val newRoleCode = null
    }

    @Serializable
    data class DeletedNote(
        override val initiatedUserId: String,
        override val affectedNoteId: String
    ): NotesListSocketEvent() {
        override val eventCode = EVENT_DELETED_NOTE
        override val newRoleCode = null
        override val newNoteInfoModel: NoteCompactInfoDto? = null
        override val updateNoteInfoModel: NoteCompactUpdateInfoDto? = null
    }

    @Serializable
    data class UpdatedRole(
        override val initiatedUserId: String,
        override val affectedNoteId: String,
        override val newRoleCode: Short
    ): NotesListSocketEvent() {
        override val eventCode = EVENT_UPDATED_ROLE
        override val newNoteInfoModel: NoteCompactInfoDto? = null
        override val updateNoteInfoModel: NoteCompactUpdateInfoDto? = null
    }

}
