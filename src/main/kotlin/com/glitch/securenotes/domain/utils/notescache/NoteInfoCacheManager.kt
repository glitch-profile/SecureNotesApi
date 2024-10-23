package com.glitch.securenotes.domain.utils.notescache

import com.glitch.securenotes.data.model.entity.FileModel

/**
 * Util class that stores information about the last requested user notes (usually 1024).
 * Allows access to the encryption key, creator id and other shared user ids, and information about stored resources.
 * Does not contain the content of the note itself
 */
interface NoteInfoCacheManager {

    fun isNoteInfoExists(noteId: String): Boolean

    fun getNoteInfo(noteId: String): NoteInfoCache

    fun getNoteInfo(noteId: String, requestedUserId: String): NoteInfoCache

    // add to cache

    fun putNoteInfo(
        noteId: String,
        creatorId: String,
        editorsSharedIds: Set<String> = emptySet(),
        readersSharedIds: Set<String> = emptySet(),
        decryptedEncryptionKey: String,
        decryptedNoteResource: List<FileModel> = emptyList()
    )

    // users

    fun getUserRoleLevel(noteId: String, userId: String): Short

    fun getUserRoleLevel(note: NoteInfoCache, userId: String): Short

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

    fun getResourceById(noteId: String, resourceIds: List<String>, requestedUserId: String): List<FileModel>

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

    // remove from cache

    fun deleteNoteInfo(
        noteId: String
    )

    // utils

    fun updateLastActiveTimestamp(
        noteId: String
    )

}