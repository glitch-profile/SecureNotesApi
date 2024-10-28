package com.glitch.securenotes.data.datasourceimpl.notes

import com.glitch.floweryapi.domain.utils.encryptor.AESEncryptor
import com.glitch.securenotes.data.datasource.notes.NoteResourcesDataSource
import com.glitch.securenotes.data.datasource.notes.NotesDataSource
import com.glitch.securenotes.data.exceptions.notes.NoPermissionForEditException
import com.glitch.securenotes.data.exceptions.notes.NoPermissionForReadException
import com.glitch.securenotes.data.exceptions.resources.ResourceNotFoundException
import com.glitch.securenotes.data.model.entity.FileModel
import com.glitch.securenotes.data.model.entity.ResourceModel
import com.glitch.securenotes.domain.utils.filemanager.FileManager
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import java.time.OffsetDateTime
import java.time.ZoneId

class NotesResourcesDataSourceImpl(
    db: MongoDatabase,
    private val notes: NotesDataSource,
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

    private suspend fun getResourceById(noteId: String, resourceId: String): ResourceModel {
        val filter = Filters.and(
            Filters.eq(ResourceModel::noteId.name, noteId),
            Filters.eq("_id", resourceId)
        )
        return resources.find(filter).singleOrNull()
            ?: throw ResourceNotFoundException()
    }

    override suspend fun getResourceById(
        noteId: String,
        resourceId: String,
        requestedUserId: String,
        returnDecrypted: Boolean
    ): ResourceModel {
        checkUserReadPermission(noteId, requestedUserId)
        val noteDecryptionKey = notes.getNoteById(noteId, requestedUserId, false).encryptionKey
        val decryptedKey = AESEncryptor.decrypt(noteDecryptionKey)
        val resource = decryptResource(getResourceById(noteId = noteId, resourceId = resourceId), decryptedKey)
        return if (returnDecrypted) decryptResource(resource, decryptedKey) else resource
    }

    private suspend fun getResourcesById(noteId: String, resourceIds: Set<String>): List<ResourceModel> {
        val filter = Filters.and(
            Filters.eq(ResourceModel::noteId.name, noteId),
            Filters.`in`("_id", resourceIds)
        )
        return resources.find(filter)
            .sort(Sorts.descending(ResourceModel::createdTimestamp.name))
            .toList()
    }

    override suspend fun getResourcesById(
        noteId: String,
        resourceIds: Set<String>,
        requestedUserId: String,
        returnDecrypted: Boolean
    ): List<ResourceModel> {
        checkUserReadPermission(noteId, requestedUserId)
        val noteDecryptionKey = notes.getNoteById(noteId, requestedUserId, false).encryptionKey
        val decryptedKey = AESEncryptor.decrypt(noteDecryptionKey)
        val resources = getResourcesById(noteId = noteId, resourceIds = resourceIds)
        return if (returnDecrypted) {
            resources.map { decryptResource(it, decryptedKey) }
        } else resources
    }

    private suspend fun getResourcesForNote(noteId: String): List<ResourceModel> {
        val filter = Filters.eq(ResourceModel::noteId.name, noteId)
        val result = resources.find(filter)
            .sort(Sorts.descending(ResourceModel::createdTimestamp.name))
            .toList()
        return result
    }

    override suspend fun getResourcesForNote(
        noteId: String,
        requestedUserId: String,
        returnDecrypted: Boolean
    ): List<ResourceModel> {
        checkUserReadPermission(noteId, requestedUserId)
        val noteDecryptionKey = notes.getNoteById(noteId, requestedUserId, false)
            .encryptionKey
        val decryptedKey = AESEncryptor.decrypt(noteDecryptionKey)
        val resources = getResourcesForNote(noteId)
        return if (returnDecrypted) {
            resources.map { decryptResource(it, decryptedKey) }
        } else resources
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
        val noteEncryptionKey = notes.getNoteById(noteId, editorUserId, false).encryptionKey
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
        // TODO: Add save to cache
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
        val noteEncryptionKey = notes.getNoteById(noteId, editorUserId, false).encryptionKey
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
        val result = resources.updateOne(filter, update)
        return result.modifiedCount != 0L
    }

    override suspend fun updateResourceDescription(
        noteId: String,
        resourceId: String,
        editorUserId: String,
        newDescription: String?
    ): Boolean {
        checkUserEditPermission(noteId, editorUserId)
        val updateDescription = if (newDescription.isNullOrBlank()) {
            Updates.set(ResourceModel::description.name, null)
        } else {
            val noteEncryptionKey = notes.getNoteById(noteId, editorUserId, false).encryptionKey
            val encryptionKey = AESEncryptor.decrypt(noteEncryptionKey)
            val newDescriptionEncrypted = AESEncryptor.encrypt(newDescription, encryptionKey)
            Updates.set(ResourceModel::description.name, newDescriptionEncrypted)
        }
        val filter = Filters.and(
            Filters.eq("_id", resourceId),
            Filters.eq(ResourceModel::noteId.name, noteId)
        )
        val currentTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
        val update = Updates.combine(
            updateDescription,
            Updates.set(ResourceModel::lastEditTimestamp.name, currentTimestamp)
        )
        val result = resources.updateOne(filter, update)
        return result.modifiedCount != 0L
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
        val resource = getResourceById(noteId, resourceId)
        deleteFilesForResource(resource.file)
        val filter = Filters.and(
            Filters.eq("_id", resourceId),
            Filters.eq(ResourceModel::noteId.name, noteId)
        )
        val result = resources.deleteOne(filter)
        return result.deletedCount != 0L
    }

    override suspend fun deleteResourceByIds(noteId: String, editorUserId: String, resourceIds: Set<String>): Boolean {
        checkUserEditPermission(noteId, editorUserId)
        val resourceList = getResourcesById(noteId, resourceIds = resourceIds)
        resourceList.forEach { deleteFilesForResource(it.file) }
        val filter = Filters.and(
            Filters.`in`("_id", resourceIds),
            Filters.eq(ResourceModel::noteId.name, noteId)
        )
        val result = resources.deleteMany(filter)
        return result.deletedCount != 0L
    }

    override suspend fun deleteResourceForNote(noteId: String, editorUserId: String): Boolean {
        checkUserEditPermission(noteId, editorUserId)
        val resourceList = getResourcesForNote(noteId)
        resourceList.forEach { deleteFilesForResource(it.file) }
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

    override fun encryptResource(resource: ResourceModel, encryptionKey: String): ResourceModel {
        val file = resource.file.copy(
            name = AESEncryptor.encrypt(resource.file.name, encryptionKey)
        )
        return resource.copy(
            title = AESEncryptor.encrypt(resource.title, encryptionKey),
            description = resource.description?.run { AESEncryptor.encrypt(this, encryptionKey) },
            file = file
        )
    }

    override fun decryptResource(resource: ResourceModel, decryptionKey: String): ResourceModel {
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