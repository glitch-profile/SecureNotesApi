package com.glitch.securenotes.data.datasourceimpl.notes

import com.glitch.floweryapi.domain.utils.encryptor.AESEncryptor
import com.glitch.securenotes.data.cache.datacache.NoteResourcesDataCache
import com.glitch.securenotes.data.datasource.notes.NoteResourcesDataSource
import com.glitch.securenotes.data.datasource.notes.NotesDataSource
import com.glitch.securenotes.data.exceptions.notes.NoPermissionForEditException
import com.glitch.securenotes.data.exceptions.notes.NoPermissionForReadException
import com.glitch.securenotes.data.exceptions.resources.ResourceNotFoundException
import com.glitch.securenotes.data.model.entity.FileModel
import com.glitch.securenotes.data.model.entity.ResourceModel
import com.glitch.securenotes.domain.utils.filemanager.FileManager
import com.mongodb.client.model.*
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import java.time.OffsetDateTime
import java.time.ZoneId

class NoteResourcesDataSourceImpl(
    db: MongoDatabase,
    private val notes: NotesDataSource,
    private val resourceCache: NoteResourcesDataCache,
    private val fileManager: FileManager
): NoteResourcesDataSource {

    private val resources = db.getCollection<ResourceModel>("NoteResources")

    // GET

    private suspend fun checkUserReadPermission(noteId: String, userId: String) {
        if (!notes.isNoteReadableForUser(noteId, userId))
            throw NoPermissionForReadException()
    }

    private suspend fun checkUserEditPermission(noteId: String, userId: String) {
        if (!notes.isNoteEditableForUser(noteId, userId))
            throw NoPermissionForEditException()
    }

    override suspend fun getResourceById(
        noteId: String,
        resourceId: String,
        requestedUserId: String,
    ): ResourceModel {
        checkUserReadPermission(noteId, requestedUserId)
        if (resourceCache.isResourceForNoteSaved(noteId, resourceId)) {
            return resourceCache.getResourceById(noteId, resourceId)!!
        } else {
            val allResourcesForNote = getResourcesForNote(noteId, requestedUserId)
            return allResourcesForNote.firstOrNull { it.id == resourceId }
                ?: throw ResourceNotFoundException()
        }
    }

    override suspend fun getResourcesById(
        noteId: String,
        resourceIds: Set<String>,
        requestedUserId: String
    ): List<ResourceModel> {
        checkUserReadPermission(noteId, requestedUserId)
        if (resourceCache.isNoteKeyExists(noteId)) {
            return resourceCache.getResourcesByIds(noteId, resourceIds.toList())!!
        } else {
            val allResources = getResourcesForNote(noteId, requestedUserId)
            return allResources.filter { resourceIds.contains(it.id) }
        }
    }

    override suspend fun getResourcesForNote(
        noteId: String,
        requestedUserId: String
    ): List<ResourceModel> {
        checkUserReadPermission(noteId, requestedUserId)
        if (resourceCache.isNoteKeyExists(noteId)) {
            return resourceCache.getResourcesForNote(noteId)!!
        } else {
            val noteDecryptionKey = notes.getNoteById(noteId, requestedUserId)
                .encryptionKey
            val decryptedKey = AESEncryptor.decrypt(noteDecryptionKey)
            val filters = Filters.eq(ResourceModel::noteId.name, noteId)
            val foundedResources = resources.find(filters)
                .sort(Sorts.descending(ResourceModel::createdTimestamp.name))
                .map { decryptResource(it, decryptedKey) }
                .toList()
            resourceCache.saveResourcesToCache(noteId, foundedResources)
            return foundedResources
        }
    }

    // ADD

    override suspend fun addResourceForNote(
        noteId: String,
        editorUserId: String,
        title: String,
        description: String?,
        fileModel: FileModel
    ): ResourceModel {
        checkUserEditPermission(noteId, editorUserId)
        val noteEncryptionKey = notes.getNoteById(noteId, editorUserId).encryptionKey
        val encryptionKey = AESEncryptor.decrypt(noteEncryptionKey)
        val resource = ResourceModel(
            noteId = noteId,
            title = title,
            description = description,
            file = fileModel,
            createdUserId = editorUserId
        )
        val encryptedResource = encryptResource(resource, encryptionKey)
        resources.insertOne(encryptedResource)
        resourceCache.addResourceToNote(noteId, resource)
        return resource
    }

    // UPDATE

    override suspend fun updateResourceTitle(
        noteId: String,
        resourceId: String,
        editorUserId: String,
        newTitle: String
    ): Boolean {
        checkUserEditPermission(noteId, editorUserId)
        val noteEncryptionKey = notes.getNoteById(noteId, editorUserId).encryptionKey
        val encryptionKey = AESEncryptor.decrypt(noteEncryptionKey)
        val newTitleEncrypted = AESEncryptor.encrypt(newTitle, encryptionKey)
        val filter = Filters.and(
            Filters.eq("_id", resourceId),
            Filters.eq(ResourceModel::noteId.name, noteId)
        )
        val currentTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
        val update = Updates.combine(
            Updates.set(ResourceModel::title.name, newTitleEncrypted),
            Updates.set(ResourceModel::lastEditTimestamp.name, currentTimestamp)
        )
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = resources.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            resourceCache.updateResourceForNote(noteId, decryptResource(result, encryptionKey))
        }
        return result != null
    }

    override suspend fun updateResourceDescription(
        noteId: String,
        resourceId: String,
        editorUserId: String,
        newDescription: String?
    ): Boolean {
        checkUserEditPermission(noteId, editorUserId)
        val filter = Filters.and(
            Filters.eq("_id", resourceId),
            Filters.eq(ResourceModel::noteId.name, noteId)
        )
        val noteEncryptionKey = notes.getNoteById(noteId, editorUserId).encryptionKey
        val encryptionKey = AESEncryptor.decrypt(noteEncryptionKey)
        val updateDescription = if (newDescription.isNullOrBlank()) {
            Updates.set(ResourceModel::description.name, null)
        } else {
            val newDescriptionEncrypted = AESEncryptor.encrypt(newDescription, encryptionKey)
            Updates.set(ResourceModel::description.name, newDescriptionEncrypted)
        }
        val currentTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
        val update = Updates.combine(
            updateDescription,
            Updates.set(ResourceModel::lastEditTimestamp.name, currentTimestamp)
        )
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = resources.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            resourceCache.updateResourceForNote(noteId, decryptResource(result, encryptionKey))
        }
        return result != null
    }

    // DELETE

    private fun deleteFilesForResource(file: FileModel): Boolean {
        val mainLocalPath = fileManager.toLocalPath(file.urlPath)
        val mainDeleteResult = fileManager.deleteFile(mainLocalPath)
        if (file.previewUrlPath != null) {
            val previewLocalPath = fileManager.toLocalPath(file.previewUrlPath)
            val previewDeleteResult = fileManager.deleteFile(previewLocalPath)
            return mainDeleteResult || previewDeleteResult
        } else return mainDeleteResult
    }

    override suspend fun deleteResourceById(noteId: String, editorUserId: String, resourceId: String): Boolean {
        checkUserEditPermission(noteId, editorUserId)
        val resource = getResourceById(noteId, resourceId, editorUserId)
        deleteFilesForResource(resource.file)
        resourceCache.deleteResourceFromNote(noteId, resourceId)
        val filter = Filters.and(
            Filters.eq("_id", resourceId),
            Filters.eq(ResourceModel::noteId.name, noteId)
        )
        val result = resources.deleteOne(filter)
        return result.deletedCount != 0L
    }

    override suspend fun deleteResourceByIds(noteId: String, editorUserId: String, resourceIds: Set<String>): Boolean {
        checkUserEditPermission(noteId, editorUserId)
        val resourceList = getResourcesById(noteId, resourceIds, editorUserId)
        resourceList.forEach { deleteFilesForResource(it.file) }
        resourceCache.deleteResourcesFromNote(noteId, resourceIds.toList())
        val filter = Filters.and(
            Filters.`in`("_id", resourceIds),
            Filters.eq(ResourceModel::noteId.name, noteId)
        )
        val result = resources.deleteMany(filter)
        return result.deletedCount != 0L
    }

    override suspend fun deleteResourceForNote(noteId: String, editorUserId: String): Boolean {
        checkUserEditPermission(noteId, editorUserId)
        val resourceList = getResourcesForNote(noteId, editorUserId)
        resourceList.forEach { deleteFilesForResource(it.file) }
        resourceCache.deleteNoteFromCache(noteId)
        val filter = Filters.and(
            Filters.`in`(ResourceModel::noteId.name, noteId)
        )
        val result = resources.deleteMany(filter)
        return result.deletedCount != 0L
    }

    override suspend fun deleteResourceForNotes(noteIds: Set<String>, editorUserId: String) {
        noteIds.forEach {
            deleteResourceForNote(noteId = it, editorUserId = editorUserId)
        }
    }

    // UTILS

    private fun encryptResource(resource: ResourceModel, encryptionKey: String): ResourceModel {
        val file = resource.file.copy(
            name = AESEncryptor.encrypt(resource.file.name, encryptionKey)
        )
        return resource.copy(
            title = AESEncryptor.encrypt(resource.title, encryptionKey),
            description = resource.description?.run { AESEncryptor.encrypt(this, encryptionKey) },
            file = file
        )
    }

    private fun decryptResource(resource: ResourceModel, decryptionKey: String): ResourceModel {
        val file = resource.file.copy(
            name = AESEncryptor.decrypt(resource.file.name, decryptionKey)
        )
        return resource.copy(
            title = AESEncryptor.decrypt(resource.title, decryptionKey),
            description = resource.description?.run { AESEncryptor.decrypt(this, decryptionKey) },
            file = file
        )
    }
}