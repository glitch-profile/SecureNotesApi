package com.glitch.securenotes.data.datasource.notes

import com.glitch.securenotes.data.model.entity.FileModel

interface NoteResourcesManager {

    // GET

    suspend fun getResourceById(
        noteId: String,
        requestedUserId: String,
        resourceId: String
    ): FileModel

    suspend fun getResourceByIds(
        noteId: String,
        requestedUserId: String,
        resourceIds: List<String>
    ): List<FileModel>

    suspend fun getResourceForNote(
        noteId: String,
        requestedUserId: String
    ): List<FileModel>

    suspend fun getResourceForNotes(
        noteIds: List<String>,
        requestedUserId: String
    ): List<FileModel>

    // CREATE

    suspend fun addResourceToNote(
        noteId: String,
        editorUserId: String,
        fileName: String,
        fileUrl: String,
        fileDescription: String? = null,
        filePreviewUrl: String? = null
    ): FileModel

    // UPDATE

    suspend fun updateResourceTitle(
        noteId: String,
        editorUserId: String,
        resourceId: String,
        newResourceTitle: String?
    ): Boolean

    suspend fun updateResourceDescription(
        noteId: String,
        editorUserId: String,
        resourceId: String,
        newResourceDescription: String?
    ): Boolean

    // DELETE

    suspend fun deleteResourceById(
        noteId: String,
        editorUserId: String,
        resourceId: String
    ): Boolean

    suspend fun deleteResourceByIds(
        noteId: String,
        editorUserId: String,
        resourceIds: List<String>
    ): Boolean

    suspend fun deleteResourceForNote(
        noteId: String,
        editorUserId: String
    ): Boolean

    suspend fun deleteResourceForNotes(
        noteIds: List<String>,
        editorUserId: String
    )

    // UTILS

    suspend fun encryptResource(
        fileModel: FileModel,
        encryptionKey: String
    ): FileModel

    suspend fun decryptResource(
        fileModel: FileModel,
        decryptionKey: String
    ): FileModel

}