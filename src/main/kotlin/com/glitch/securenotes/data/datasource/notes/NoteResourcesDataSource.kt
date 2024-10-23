package com.glitch.securenotes.data.datasource.notes

import com.glitch.securenotes.data.model.entity.FileModel
import com.glitch.securenotes.data.model.entity.ResourceModel

interface NoteResourcesDataSource {

    // GET

    suspend fun getResourceById(
        resourceId: String
    ): ResourceModel

    suspend fun getResourceById(
        noteId: String,
        resourceId: String,
        requestedUserId: String
    ): ResourceModel

    suspend fun getResourcesById(
        resourceIds: Set<String>
    ): List<ResourceModel>

    suspend fun getResourcesById(
        noteId: String,
        resourceIds: Set<String>,
        requestedUserId: String
    ): List<ResourceModel>

    suspend fun getResourcesForNote(
        noteId: String,
        requestedUserId: String
    ): List<ResourceModel>

    // ADD

    suspend fun addResourceForNote(
        noteId: String,
        editorUserId: String,
        title: String,
        description: String?,
        fileModel: FileModel
    ): ResourceModel

    suspend fun copyResourceFromNote(
        targetNoteId: String,
        editorUserId: String,
        sourceNoteId: String,
        sourceResourceId: String
    ): ResourceModel

    // UPDATE

    suspend fun updateResourceTitle(
        noteId: String,
        resourceId: String,
        editorUserId: String,
        newTitle: String
    ): Boolean

    suspend fun updateResourceDescription(
        noteId: String,
        resourceId: String,
        editorUserId: String,
        newDescription: String?
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

    fun encryptResource(
        resource: ResourceModel,
        encryptionKey: String
    ): ResourceModel

    fun decryptResource(
        resource: ResourceModel,
        decryptionKey: String
    )

}