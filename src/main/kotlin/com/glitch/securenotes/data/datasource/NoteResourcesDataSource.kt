package com.glitch.securenotes.data.datasource

import com.glitch.securenotes.data.model.entity.FileModel

interface NoteResourcesDataSource {

    suspend fun getResourceById(resourceId: String): FileModel

    suspend fun getResourceById(noteId: String, resourceId: String): FileModel

    suspend fun getResourcesForNote(noteId: String): List<FileModel>

    suspend fun getResourcesForNotes(noteIds: List<String>): List<FileModel>

    suspend fun getResourcesForUser(userId: String): List<FileModel>

    suspend fun addResourceToNote(
        noteId: String,
        fileName: String,
        fileDescription: String? = null,
        fileUrlPath: String,
        previewFileUrlPath: String? = null
    ): FileModel

    suspend fun deleteResourceFromNote(
        noteId: String,
        resourceId: String
    )

    suspend fun updateResourceDescription(
        noteId: String,
        resourceId: String,
        newTitle: String,
        newDescription: String? = null
    )

}