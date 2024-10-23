package com.glitch.securenotes.data.datasourceimpl.notes

import com.glitch.floweryapi.domain.utils.encryptor.AESEncryptor
import com.glitch.securenotes.data.datasource.notes.NoteResourcesManager
import com.glitch.securenotes.data.datasource.notes.NotesDataSource
import com.glitch.securenotes.data.exceptions.notes.ResourceNotFoundException
import com.glitch.securenotes.data.model.entity.FileModel
import com.glitch.securenotes.data.model.entity.NoteModel
import com.glitch.securenotes.domain.utils.notescache.NoteInfoCache
import com.glitch.securenotes.domain.utils.notescache.NoteInfoCacheManager
import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase

class NoteResourceManagerImpl(
    db: MongoDatabase,
    private val notes: NotesDataSource,
    private val notesCache: NoteInfoCacheManager
): NoteResourcesManager {

    override suspend fun getResourceById(noteId: String, requestedUserId: String, resourceId: String): FileModel {
        if (notesCache.isNoteInfoExists(noteId)) {
            return notesCache.getResourceById(
                noteId = noteId,
                resourceId = resourceId,
                requestedUserId = requestedUserId
            )
        } else {
            val note = notes.getNoteById(noteId, requestedUserId)
            if (note.noteResources.any { it.fileId == resourceId }) {
                val encryptionKey = AESEncryptor.decrypt(note.encryptionKey)
                val decryptedNoteResources = note.noteResources.map { decryptResource(it, encryptionKey) }
                notesCache.putNoteInfo(
                    noteId = note.id,
                    creatorId = note.creatorId,
                    editorsSharedIds = note.sharedEditorUserIds,
                    readersSharedIds = note.sharedReaderUserIds,
                    decryptedEncryptionKey = encryptionKey,
                    decryptedNoteResource = decryptedNoteResources
                )
                return decryptedNoteResources.first { it.fileId == resourceId }
            } else {
                throw ResourceNotFoundException()
            }
        }
    }

    override suspend fun getResourceByIds(
        noteId: String,
        requestedUserId: String,
        resourceIds: List<String>
    ): List<FileModel> {
        if (notesCache.isNoteInfoExists(noteId)) {
            return notesCache.getResourceById(
                noteId = noteId,
                resourceIds = resourceIds,
                requestedUserId = requestedUserId
            )
        } else {
            val note = notes.getNoteById(noteId, requestedUserId)
            if (note.noteResources.any { resourceIds.contains(it.fileId) }) {
                val encryptionKey = AESEncryptor.decrypt(note.encryptionKey)
                val decryptedNoteResources = note.noteResources.map { decryptResource(it, encryptionKey) }
                notesCache.putNoteInfo(
                    noteId = note.id,
                    creatorId = note.creatorId,
                    editorsSharedIds = note.sharedEditorUserIds,
                    readersSharedIds = note.sharedReaderUserIds,
                    decryptedEncryptionKey = encryptionKey,
                    decryptedNoteResource = decryptedNoteResources
                )
                return decryptedNoteResources.filter { resourceIds.contains(it.fileId) }
            } else {
                throw ResourceNotFoundException()
            }
        }
    }

    override suspend fun getResourceForNote(noteId: String, requestedUserId: String): List<FileModel> {
        if (notesCache.isNoteInfoExists(noteId)) {
            return notesCache.getResourcesForNote(noteId = noteId, requestedUserId = requestedUserId)
        } else {
            val note = notes.getNoteById(noteId, requestedUserId)
            val encryptionKey = AESEncryptor.decrypt(note.encryptionKey)
            val decryptedNoteResources = note.noteResources.map { decryptResource(it, encryptionKey) }
            notesCache.putNoteInfo(
                noteId = note.id,
                creatorId = note.creatorId,
                editorsSharedIds = note.sharedEditorUserIds,
                readersSharedIds = note.sharedReaderUserIds,
                decryptedEncryptionKey = encryptionKey,
                decryptedNoteResource = decryptedNoteResources
            )
            return decryptedNoteResources
        }
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
        val encryptionKey = if (notesCache.isNoteInfoExists(noteId)) {
            notesCache.getEncryptionKeyForNote(noteId, editorUserId)
        } else {
            val note = notes.getNoteById(noteId = noteId, requestedUserId = editorUserId)
            AESEncryptor.decrypt(note.encryptionKey)
        }
        val resourceModel = FileModel(
            name = fileName,
            description = fileDescription,
            urlPath = fileUrl,
            previewUrlPath = filePreviewUrl
        )
        val encryptedResourceModel = encryptResource(resourceModel, encryptionKey)
        TODO()
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

    // UTILS

    override suspend fun encryptResource(fileModel: FileModel, encryptionKey: String): FileModel {
        return fileModel.copy(
            name = AESEncryptor.encrypt(fileModel.name, encryptionKey),
            description = fileModel.description?.run { AESEncryptor.encrypt(this, encryptionKey) }
        )
    }

    override suspend fun decryptResource(fileModel: FileModel, decryptionKey: String): FileModel {
        return fileModel.copy(
            name = AESEncryptor.decrypt(fileModel.name, decryptionKey),
            description = fileModel.description?.run { AESEncryptor.decrypt(this, decryptionKey) }
        )
    }
}