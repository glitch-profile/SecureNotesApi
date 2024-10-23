package com.glitch.securenotes.data.datasourceimpl.notes

import com.glitch.securenotes.data.datasource.notes.NoteResourcesManager
import com.glitch.securenotes.data.model.entity.FileModel
import com.glitch.securenotes.domain.utils.notescache.NoteInfoCacheManager

class NoteResourceManagerImpl(
    private val notesCache: NoteInfoCacheManager
): NoteResourcesManager {

    override suspend fun getResourceById(noteId: String, requestedUserId: String, resourceId: String): FileModel {
        TODO("Not yet implemented")
    }

    override suspend fun getResourceByIds(
        noteId: String,
        requestedUserId: String,
        resourceIds: List<String>
    ): List<FileModel> {
        TODO("Not yet implemented")
    }

    override suspend fun getResourceForNote(noteId: String, requestedUserId: String): List<FileModel> {
        TODO("Not yet implemented")
    }

    override suspend fun getResourceForNotes(noteIds: List<String>, requestedUserId: String): List<FileModel> {
        TODO("Not yet implemented")
    }

    override suspend fun addResourceToNote(
        noteId: String,
        editorUserId: String,
        fileName: String,
        fileUrl: String,
        fileDescription: String?,
        filePreviewUrl: String?
    ): FileModel {
        TODO("Not yet implemented")
    }

    override suspend fun updateResourceTitle(
        noteId: String,
        editorUserId: String,
        resourceId: String,
        newResourceTitle: String?
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun updateResourceDescription(
        noteId: String,
        editorUserId: String,
        resourceId: String,
        newResourceDescription: String?
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun deleteResourceById(noteId: String, editorUserId: String, resourceId: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun deleteResourceByIds(noteId: String, editorUserId: String, resourceIds: List<String>): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun deleteResourceForNote(noteId: String, editorUserId: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun deleteResourceForNotes(noteIds: List<String>, editorUserId: String) {
        TODO("Not yet implemented")
    }
}