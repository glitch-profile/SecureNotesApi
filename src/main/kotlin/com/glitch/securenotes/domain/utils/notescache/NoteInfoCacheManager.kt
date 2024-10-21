package com.glitch.securenotes.domain.utils.notescache

import com.glitch.securenotes.data.model.entity.FileModel

interface NoteInfoCacheManager {

    fun isNoteInfoExists(noteId: String): Boolean

    fun getNoteInfo(noteId: String): NoteInfoCache

    fun putNoteInfo(
        noteId: String,
        creatorId: String,
        decryptedEncryptionKey: String,
        editorsSharedIds: List<String> = emptyList(),
        readersSharedIds: List<String> = emptyList(),
        noteResource: List<FileModel> = emptyList()
    )

    // users

    fun getUserRole(noteId: String, userId: String): UserRole

    fun getUserRole(note: NoteInfoCache, userId: String): UserRole

    fun updateOwnerId(noteId: String, userId: String)

    fun addEditorUserId(noteId: String, userId: String)

    fun removeEditorUserId(noteId: String, userId: String)

    fun updateUserRoleToReader(noteId: String, userId: String)

    fun addReaderUserId(noteId: String, userId: String)

    fun removeReaderUserId(noteId: String, userId: String)

    fun updateUserRoleToEditor(noteId: String, userId: String)

    // resources

    fun getResourcesForNote(noteId: String, requestedUserId: String): List<FileModel>

    fun getResourceById(noteId: String, resourceId: String, requestedUserId: String): FileModel

    fun addResourceToNote(noteId: String, requestedUserId: String, resource: FileModel)

    fun updateResourceTitle(
        noteId: String,
        resourceId: String,
        newFileName: String
    )

    fun updateResourceDescription(
        noteId: String,
        resourceId: String,
        newDescription: String?
    )

    fun deleteResource(noteId: String, resourceId: String)

    fun getEncryptionKeyForNote(noteId: String, requestedUserId: String): String

}