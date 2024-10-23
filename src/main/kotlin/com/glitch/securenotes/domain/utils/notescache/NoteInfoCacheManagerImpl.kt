package com.glitch.securenotes.domain.utils.notescache

import com.glitch.securenotes.data.model.entity.FileModel
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

class NoteInfoCacheManagerImpl: NoteInfoCacheManager {

    private val cache = ConcurrentHashMap<String, NoteInfoCache>()

    override fun isNoteInfoExists(noteId: String): Boolean {
        return cache.containsKey(noteId)
    }

    override fun getNoteInfo(noteId: String): NoteInfoCache {
        if (isNoteInfoExists(noteId)) return cache[noteId]!!
        else throw NoteCacheInfoNoteNotFoundExtension()
    }

    override fun getNoteInfo(noteId: String, requestedUserId: String): NoteInfoCache {
        if (isNoteInfoExists(noteId)) {
            val note = getNoteInfo(noteId)
            if (getUserRoleLevel(note, requestedUserId) == UserRole.UNKNOWN) {
                return note
            } else throw NoteCachePermissionsException()
        } else throw NoteCacheInfoNoteNotFoundExtension()
    }

    override fun putNoteInfo(
        noteId: String,
        creatorId: String,
        editorsSharedIds: Set<String>,
        readersSharedIds: Set<String>,
        decryptedEncryptionKey: String,
        decryptedNoteResource: List<FileModel>
    ) {
        val noteInfo = NoteInfoCache(
            creatorId = creatorId,
            editorUserIds = editorsSharedIds,
            readerUserIds = readersSharedIds,
            noteEncryptionKey = decryptedEncryptionKey,
            noteResource = decryptedNoteResource
        )
        if (cache.size >= 1024) {
            val oldestResource = cache.minByOrNull { it.value.cacheLastActiveTimestamp }!!
            cache.remove(oldestResource.key)
        }
        cache[noteId] = noteInfo
    }

    override fun getUserRoleLevel(noteId: String, userId: String): Short {
        val noteInfo = getNoteInfo(noteId)
        return if (noteInfo.creatorId == userId) UserRole.OWNER
        else if (noteInfo.editorUserIds.contains(userId)) UserRole.EDITOR
        else if (noteInfo.readerUserIds.contains(userId)) UserRole.READER
        else UserRole.UNKNOWN
    }

    override fun getUserRoleLevel(note: NoteInfoCache, userId: String): Short {
        return if (note.creatorId == userId) UserRole.OWNER
        else if (note.editorUserIds.contains(userId)) UserRole.EDITOR
        else if (note.readerUserIds.contains(userId)) UserRole.READER
        else UserRole.UNKNOWN
    }

    override fun updateOwnerId(noteId: String, userId: String) {
        val note = getNoteInfo(noteId)
        val currentOwner = note.creatorId
        val newEditorsList = note.editorUserIds.toMutableSet().apply {
            add(currentOwner)
        }
        val newNote = note.copy(
            creatorId = userId,
            editorUserIds = newEditorsList,
            cacheLastActiveTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
        )
        cache[noteId] = newNote
    }

    override fun addEditorUserId(noteId: String, userId: String) {
        val note = getNoteInfo(noteId)
        cache[noteId] = note.copy(
            editorUserIds = note.editorUserIds.toMutableSet().apply {
                add(userId)
            },
            cacheLastActiveTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
        )
    }

    override fun removeEditorUserId(noteId: String, userId: String) {
        val note = getNoteInfo(noteId)
        cache[noteId] = note.copy(
            editorUserIds = note.editorUserIds.toMutableSet().apply {
                remove(userId)
            },
            cacheLastActiveTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
        )
    }

    override fun updateUserRoleToReader(noteId: String, userId: String) {
        val note = getNoteInfo(noteId)
        cache[noteId] = note.copy(
            editorUserIds = note.editorUserIds.toMutableSet().apply {
                remove(userId)
            },
            readerUserIds = note.readerUserIds.toMutableSet().apply {
                add(userId)
            },
            cacheLastActiveTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
        )
    }

    override fun addReaderUserId(noteId: String, userId: String) {
        val note = getNoteInfo(noteId)
        cache[noteId] = note.copy(
            readerUserIds = note.readerUserIds.toMutableSet().apply {
                add(userId)
            },
            cacheLastActiveTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
        )
    }

    override fun removeReaderUserId(noteId: String, userId: String) {
        val note = getNoteInfo(noteId)
        cache[noteId] = note.copy(
            readerUserIds = note.readerUserIds.toMutableSet().apply {
                remove(userId)
            },
            cacheLastActiveTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
        )
    }

    override fun updateUserRoleToEditor(noteId: String, userId: String) {
        val note = getNoteInfo(noteId)
        cache[noteId] = note.copy(
            editorUserIds = note.editorUserIds.toMutableSet().apply {
                add(userId)
            },
            readerUserIds = note.readerUserIds.toMutableSet().apply {
                remove(userId)
            },
            cacheLastActiveTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
        )
    }

    override fun getResourcesForNote(noteId: String, requestedUserId: String): List<FileModel> {
        val note = getNoteInfo(noteId, requestedUserId)
        cache[noteId] = note.copy(
            cacheLastActiveTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
        )
        return note.noteResource
    }

    override fun getResourceById(noteId: String, resourceId: String, requestedUserId: String): FileModel {
        val note = getNoteInfo(noteId, requestedUserId)
        val requestedResource = note.noteResource.firstOrNull { it.fileId == resourceId }
            ?: throw NoteCacheResourceNotFoundException()
        cache[noteId] = note.copy(
            cacheLastActiveTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
        )
        return requestedResource
    }

    override fun getResourceById(noteId: String, resourceIds: List<String>, requestedUserId: String): List<FileModel> {
        val note = getNoteInfo(noteId, requestedUserId)
        val requestedResources = note.noteResource.filter { resourceIds.contains(it.fileId) }
        cache[noteId] = note.copy(
            cacheLastActiveTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
        )
        return requestedResources
    }

    override fun addResourceToNote(noteId: String, requestedUserId: String, resource: FileModel) {
        val note = getNoteInfo(noteId)
        cache[noteId] = note.copy(
            noteResource = note.noteResource.toMutableList().apply {
                add(resource)
            },
            cacheLastActiveTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
        )
    }

    override fun updateResourceTitle(noteId: String, resourceId: String, newFileName: String) {
        val note = getNoteInfo(noteId)
        val index = note.noteResource.indexOfFirst { it.fileId == resourceId }
        if (index == -1) return
        val oldResource = note.noteResource[index]
        val updatedResource = oldResource.copy(name = newFileName)
        cache[noteId] = note.copy(
            noteResource = note.noteResource.toMutableList().apply {
                set(index, updatedResource)
            },
            cacheLastActiveTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
        )
    }

    override fun updateResourceDescription(noteId: String, resourceId: String, newDescription: String?) {
        val note = getNoteInfo(noteId)
        val index = note.noteResource.indexOfFirst { it.fileId == resourceId }
        if (index == -1) return
        val oldResource = note.noteResource[index]
        val updatedResource = oldResource.copy(description = newDescription)
        cache[noteId] = note.copy(
            noteResource = note.noteResource.toMutableList().apply {
                set(index, updatedResource)
            },
            cacheLastActiveTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
        )
    }

    override fun deleteResource(noteId: String, resourceId: String) {
        val note = getNoteInfo(noteId)
        val index = note.noteResource.indexOfFirst { it.fileId == resourceId }
        if (index == -1) return
        cache[noteId] = note.copy(
            noteResource = note.noteResource.toMutableList().apply {
                removeAt(index)
            },
            cacheLastActiveTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
        )
    }

    override fun getEncryptionKeyForNote(noteId: String, requestedUserId: String): String {
        val note = getNoteInfo(noteId)
        if (getUserRoleLevel(note, requestedUserId) == UserRole.UNKNOWN) throw NoteCachePermissionsException()
        cache[noteId] = note.copy(
            cacheLastActiveTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
        )
        return note.noteEncryptionKey
    }

    override fun deleteNoteInfo(noteId: String) {
        cache.remove(noteId)
    }

    override fun updateLastActiveTimestamp(noteId: String) {
        if (isNoteInfoExists(noteId)) {
            val note = cache[noteId]!!
            cache[noteId] = note.copy(
                cacheLastActiveTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            )
        }
    }
}

private object UserRole {
    const val OWNER: Short = 3
    const val EDITOR: Short = 2
    const val READER: Short = 1
    const val UNKNOWN: Short = 0
}