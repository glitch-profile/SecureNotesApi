package com.glitch.securenotes.data.datasource.notes

import com.glitch.securenotes.data.model.entity.NoteModel
import java.time.OffsetDateTime
import java.time.ZoneId

interface NotesDataSource {

    // GET

    suspend fun getNotesForUser(
        userId: String,
        excludedNotesId: Set<String>,
        page: Int = 0,
        limit: Int = -1
    ): List<NoteModel>

    suspend fun getProtectedNotesForUser(
        userId: String,
        includedNotesIds: Set<String>,
        page: Int = 0,
        limit: Int = -1
    ): List<NoteModel>

    suspend fun getNotesForUser(
        userId: String,
        page: Int = 0,
        limit: Int = -1,
        onlyIncludedIds: Set<String> = emptySet(),
        excludeIds: Set<String> = emptySet()
    ): List<NoteModel>

    suspend fun getNoteById(noteId: String): NoteModel

    suspend fun getNoteById(noteId: String, requestedUserId: String): NoteModel

    suspend fun getNotesById(noteIds: Set<String>): List<NoteModel>

    suspend fun getNotesById(noteIds: Set<String>, requestedUserId: String): List<NoteModel>

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

    // info

    suspend fun updateNoteTitle(
        noteId: String,
        editorUserId: String,
        newTitle: String?
    ): Boolean

    suspend fun updateNoteDescription(
        noteId: String,
        editorUserId: String,
        newDescription: String?
    ): Boolean

    suspend fun updateNoteText(
        noteId: String,
        editorUserId: String,
        newText: String
    ): Boolean

    // note sharing

    suspend fun enableNoteSharing(noteId: String, requestedUserId: String): Boolean

    suspend fun disableNoteSharing(noteId: String, requestedUserId: String): Boolean

    suspend fun addUserToSharedEditorIds(noteId: String, requestedUserId: String, userId: String): Boolean

    suspend fun removeUserFromSharedEditorIds(noteId: String, requestedUserId: String, userId: String): Boolean

    suspend fun removeUsersFromSharedEditorIds(noteId: String, requestedUserId: String, userIds: Set<String>): Boolean

    suspend fun addUserToSharedReaderIds(noteId: String, requestedUserId: String, userId: String): Boolean

    suspend fun removeUserFromSharedReaderIds(noteId: String, requestedUserId: String, userId: String): Boolean

    suspend fun removeUsersFromSharedReaderIds(noteId: String, requestedUserId: String, userIds: Set<String>): Boolean

    suspend fun removeUserFromAllSharedNotes(userId: String): Boolean

    // users roles

    suspend fun updateNoteOwner(noteId: String, requestedUserId: String, userId: String): Boolean

    suspend fun moveUserToReaders(noteId: String, requestedUserId: String, userId: String): Boolean

    suspend fun moveUsersToReaders(noteId: String, requestedUserId: String, userIds: Set<String>): Boolean

    suspend fun moveUserToEditors(noteId: String, requestedUserId: String, userId: String): Boolean

    suspend fun moveUsersToEditors(noteId: String, requestedUserId: String, userIds: Set<String>): Boolean

    // resource ids

    suspend fun getResourceIdsForNote(noteId: String, requestedUserId: String): Set<String>

    suspend fun getResourceIdsForNotes(noteIds: Set<String>, requestedUserId: String): Set<String>

    suspend fun addResourceToNote(noteId: String, editorUserId: String, resourceId: String): Boolean

    suspend fun addResourcesToNote(noteId: String, editorUserId: String, resourceIds: Set<String>): Boolean

    suspend fun removeResourceFromNote(noteId: String, editorUserId: String, resourceId: String): Boolean

    suspend fun removeResourcesFromNote(noteId: String, editorUserId: String, resourceIds: Set<String>): Boolean

    suspend fun removeAllResourcesFromNote(noteId: String, editorUserId: String): Boolean

    // DELETE

    suspend fun deleteNoteById(noteId: String): Boolean

    suspend fun deleteNoteById(noteId: String, requestedUserId: String): Boolean

    suspend fun deleteNotesById(noteIds: Set<String>): Boolean

    suspend fun deleteNotesById(noteIds: Set<String>, requestedUserId: String): Boolean

    suspend fun deleteNoteForUser(userId: String, noteId: String): Boolean

    // including created and shared notes
    suspend fun deleteNotesForUser(
        userId: String,
        noteIds: Set<String>
    ): Boolean

    suspend fun deleteAllUserCreatedNotes(userId: String): Boolean

    suspend fun deleteAllNotesForUser(userId: String): Boolean

    // UTILS

    fun encryptNote(note: NoteModel, encryptionKey: String): NoteModel

    fun decryptNote(note: NoteModel, decryptionKey: String): NoteModel

}