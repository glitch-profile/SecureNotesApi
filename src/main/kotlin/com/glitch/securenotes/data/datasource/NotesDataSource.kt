package com.glitch.securenotes.data.datasource

import com.glitch.securenotes.data.model.entity.NoteModel
import java.time.OffsetDateTime
import java.time.ZoneId

interface NotesDataSource {

    // GET

    suspend fun getNotesForUser(
        userId: String,
        page: Int = 0,
        limit: Int = -1
    ): List<NoteModel>

    suspend fun getProtectedNotesForUser(
        userId: String,
        securedNotesPassword: String,
        page: Int = 0,
        limit: Int = -1
    ): List<NoteModel>

    suspend fun getOneNoteById(noteId: String): NoteModel

    suspend fun getManyNotesById(noteIds: List<String>): List<NoteModel>

    // ADD

    suspend fun createNewNote(
        creatorId: String,
        title: String?,
        description: String?,
        text: String
    ): NoteModel

    suspend fun createNewNote(
        creatorId: String,
        title: String?,
        description: String?,
        text: String,
        creationTimestamp: Long = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond(),
        lastEditTimestamp: Long? = null
    ): NoteModel

    // UPDATE

    // NOTES SHARING

    suspend fun enableNoteSharing(noteId: String, oldEncryptionKey: String): Boolean

    suspend fun disableNoteSharing(noteId: String, ownerEncryptionKey: String): Boolean

    suspend fun addUserToSharedIds(noteId: String, userId: String): Boolean

    suspend fun removeUserFromSharedIds(noteId: String, userId: String): Boolean

    suspend fun removeUserFromSharedIds(noteIds: List<String>, userId: String): Boolean

    suspend fun removeUserFromAllSharingNotes(userId: String): Boolean

    // DELETE

    suspend fun deleteOneNoteById(noteId: String)

    suspend fun deleteManyNotesById(noteIds: List<String>)

    suspend fun deleteAllNotesForUser(userId: String)

    // including created and shared notes
    suspend fun deleteNotesForUser(
        userId: String,
        noteIds: String
    )

}