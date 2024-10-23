package com.glitch.securenotes.data.datasourceimpl.notes

import com.glitch.securenotes.data.datasource.notes.NoteResourcesDataSource
import com.glitch.securenotes.data.model.entity.FileModel
import com.glitch.securenotes.data.model.entity.ResourceModel
import com.glitch.securenotes.domain.utils.notescache.NoteInfoCacheManager
import com.mongodb.kotlin.client.coroutine.MongoDatabase

class NotesResourcesDataSourceImpl(
    db: MongoDatabase,
    notesCache: NoteInfoCacheManager
): NoteResourcesDataSource {

    // GET

    override suspend fun getResourceById(resourceId: String): ResourceModel {
        TODO("Not yet implemented")
    }

    override suspend fun getResourceById(noteId: String, resourceId: String, requestedUserId: String): ResourceModel {
        TODO("Not yet implemented")
    }

    override suspend fun getResourcesById(resourceIds: Set<String>): List<ResourceModel> {
        TODO("Not yet implemented")
    }

    override suspend fun getResourcesById(
        noteId: String,
        resourceIds: Set<String>,
        requestedUserId: String
    ): List<ResourceModel> {
        TODO("Not yet implemented")
    }

    override suspend fun getResourcesForNote(noteId: String, requestedUserId: String): List<ResourceModel> {
        TODO("Not yet implemented")
    }

    // ADD

    override suspend fun addResourceForNote(
        noteId: String,
        editorUserId: String,
        title: String,
        description: String?,
        fileModel: FileModel
    ): ResourceModel {
        TODO("Not yet implemented")
    }

    override suspend fun copyResourceFromNote(
        targetNoteId: String,
        editorUserId: String,
        sourceNoteId: String,
        sourceResourceId: String
    ): ResourceModel {
        TODO("Not yet implemented")
    }

    // UPDATE

    override suspend fun updateResourceTitle(
        noteId: String,
        resourceId: String,
        editorUserId: String,
        newTitle: String
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun updateResourceDescription(
        noteId: String,
        resourceId: String,
        editorUserId: String,
        newDescription: String?
    ): Boolean {
        TODO("Not yet implemented")
    }

    // DELETE

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

    // UTILS

    override fun encryptResource(resource: ResourceModel, encryptionKey: String): ResourceModel {
        TODO("Not yet implemented")
    }

    override fun decryptResource(resource: ResourceModel, decryptionKey: String) {
        TODO("Not yet implemented")
    }
}