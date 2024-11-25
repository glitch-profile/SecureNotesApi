package com.glitch.securenotes.data.datasourceimpl.notes

import com.glitch.floweryapi.domain.utils.encryptor.AESEncryptor
import com.glitch.securenotes.data.cache.datacache.NotesDataCache
import com.glitch.securenotes.data.datasource.notes.NotesDataSource
import com.glitch.securenotes.data.exceptions.notes.NoPermissionForEditException
import com.glitch.securenotes.data.exceptions.notes.NoteNotFoundException
import com.glitch.securenotes.data.model.entity.NoteModel
import com.mongodb.client.model.*
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.bson.conversions.Bson
import java.time.OffsetDateTime
import java.time.ZoneId

class NotesDataSourceImpl(
    db: MongoDatabase,
    private val notesCache: NotesDataCache
): NotesDataSource {

    private val notes = db.getCollection<NoteModel>("UserNotes")

    @Deprecated(message = "use getNotesForUserV2 instead", level = DeprecationLevel.WARNING)
    override suspend fun getNotesForUserV2(
        userId: String,
        excludedNotesId: Set<String>,
        page: Int,
        limit: Int
    ): List<NoteModel> {
        val filter = Filters.and(
            Filters.not(Filters.`in`("_id", excludedNotesId)),
            Filters.or(
                Filters.eq(NoteModel::creatorId.name, userId),
                Filters.`in`(NoteModel::sharedEditorUserIds.name, userId),
                Filters.`in`(NoteModel::sharedReaderUserIds.name, userId)
            )
        )
        if (limit == -1) {
            val result = notes.find(filter)
                .sort(Sorts.descending(NoteModel::lastEditTimestamp.name))
                .toList()
            return result.map { decryptNote(it) }
        } else if (limit < 0 || page < 0) {
            throw IllegalArgumentException()
        } else {
            val result = notes.find(filter)
                .sort(Sorts.descending(NoteModel::lastEditTimestamp.name))
                .skip(page * limit)
                .limit(limit)
                .toList()
            return result.map { decryptNote(it) }
        }
    }

    @Deprecated(message = "use getNotesForUserV2 instead", level = DeprecationLevel.WARNING)
    override suspend fun getProtectedNotesForUser(
        userId: String,
        includedNotesIds: Set<String>,
        page: Int,
        limit: Int
    ): List<NoteModel> {
        val filter = Filters.and(
            Filters.`in`("_id", includedNotesIds),
            Filters.or(
                Filters.eq(NoteModel::creatorId.name, userId),
                Filters.`in`(NoteModel::sharedEditorUserIds.name, userId),
                Filters.`in`(NoteModel::sharedReaderUserIds.name, userId)
            )
        )
        if (limit == -1) {
            val result = notes.find(filter)
                .sort(Sorts.descending(NoteModel::lastEditTimestamp.name))
                .toList()
            return result.map { decryptNote(it) }
        } else if (limit < 0 || page < 0) {
            throw IllegalArgumentException()
        } else {
            val result = notes.find(filter)
                .sort(Sorts.descending(NoteModel::lastEditTimestamp.name))
                .skip(page * limit)
                .limit(limit)
                .toList()
            return result.map { decryptNote(it) }
        }
    }

    override suspend fun getNotesForUserV2(
        userId: String,
        page: Int,
        limit: Int,
        onlyIncludedIds: Set<String>,
        excludeIds: Set<String>
    ): List<NoteModel> {
        val filters = mutableListOf<Bson>().apply {
            add(
                Filters.or(
                    Filters.eq(NoteModel::creatorId.name, userId),
                    Filters.`in`(NoteModel::sharedEditorUserIds.name, userId),
                    Filters.`in`(NoteModel::sharedReaderUserIds.name, userId)
                )
            )
            if (onlyIncludedIds.isNotEmpty()) {
                add(Filters.`in`("_id", onlyIncludedIds))
            }
            if (excludeIds.isNotEmpty()) {
                add(Filters.nin("_id", excludeIds))
            }
            toList()
        }
        val searchFilters = Filters.and(filters)
        if (limit == -1) {
            val result = notes.find(searchFilters)
                .sort(Sorts.descending(NoteModel::lastEditTimestamp.name))
                .toList()
            return result.map { decryptNote(it) }
        } else if (limit < 0 || page < 0) {
            throw IllegalArgumentException()
        } else {
            val result = notes.find(searchFilters)
                .sort(Sorts.descending(NoteModel::lastEditTimestamp.name))
                .skip(page * limit)
                .limit(limit)
                .toList()
            return result.map { decryptNote(it) }
        }
    }

    @Deprecated("use other version", level = DeprecationLevel.ERROR)
    private suspend fun getNoteById(noteId: String): NoteModel {
        val filter = Filters.eq("_id", noteId)
        val result = notes.find(filter).singleOrNull() ?: throw NoteNotFoundException()
        return result
    }

    override suspend fun getNoteById(
        noteId: String,
        requestedUserId: String
    ): NoteModel {
        return if (notesCache.isNoteSaved(noteId)) {
            val foundedNote = notesCache.getNoteById(noteId)!!
            if (isUserCanRead(foundedNote, requestedUserId)) foundedNote else throw NoteNotFoundException()
        } else {
            val filter = Filters.eq("_id", noteId)
            val foundedNote = notes.find(filter).singleOrNull() ?: throw NoteNotFoundException()
            if (isUserCanRead(foundedNote, requestedUserId)) {
                val decryptedNote = decryptNote(foundedNote)
                notesCache.addNoteToCache(decryptedNote)
                decryptedNote
            } else throw NoteNotFoundException()
        }
    }

    @Deprecated("use other version", level = DeprecationLevel.ERROR)
    private suspend fun getNotesById(noteIds: Set<String>): List<NoteModel> {
        val filter = Filters.`in`("_id", noteIds)
        val result = notes.find(filter).sort(Sorts.descending(NoteModel::lastEditTimestamp.name))
            .toList()
        return result
    }

    override suspend fun getNotesById(
        noteIds: Set<String>,
        requestedUserId: String
    ): List<NoteModel> {
        val filter = Filters.and(
            Filters.`in`("_id", noteIds),
            Filters.or(
                Filters.eq(NoteModel::creatorId.name, requestedUserId),
                Filters.`in`(NoteModel::sharedEditorUserIds.name, requestedUserId),
                Filters.`in`(NoteModel::sharedReaderUserIds.name, requestedUserId)
            )
        )
        val result = notes.find(filter).sort(Sorts.descending(NoteModel::lastEditTimestamp.name))
            .toList()
        return result.map { decryptNote(it) }
    }

    // CREATE

    override suspend fun createNewNote(
        creatorId: String,
        title: String?,
        description: String?,
        text: String,
        isShared: Boolean,
        sharedEditorUserIds: Set<String>,
        sharedReaderUserIds: Set<String>,
        createdTimestamp: Long?,
        lastEditTimestamp: Long?
    ): NoteModel {
        val encryptionKey = AESEncryptor.generateSecret()
        val protectedEncryptionKey = AESEncryptor.encrypt(encryptionKey)
        val noteData = NoteModel(
            creatorId = creatorId,
            title = title,
            description = description,
            text = text,
            encryptionKey = protectedEncryptionKey,
            isSharing = isShared,
            sharedReaderUserIds = if (isShared) sharedReaderUserIds else emptySet(),
            sharedEditorUserIds = if (isShared) sharedEditorUserIds else emptySet(),
            creationTimestamp = createdTimestamp ?: OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond(),
            lastEditTimestamp = lastEditTimestamp
        )
        val encryptedNote = encryptNote(noteData, encryptionKey = encryptionKey)
        notes.insertOne(encryptedNote)
        notesCache.addNoteToCache(noteData)
        return noteData
    }

    // UPDATE

    // info
    override suspend fun updateNoteTitle(noteId: String, editorUserId: String, newTitle: String?): Boolean {
        val note = getNoteById(noteId, editorUserId)
        if (note.creatorId == editorUserId) {
            val filter = Filters.eq("_id", noteId) // only owner can update title
            val encryptionKey = AESEncryptor.decrypt(note.encryptionKey)
            val newEncryptedTitle = if (newTitle!= null) {
                AESEncryptor.encrypt(newTitle, encryptionKey)
            } else null
            val currentTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            val update = Updates.combine(
                Updates.set(NoteModel::title.name, newEncryptedTitle),
                Updates.set(NoteModel::lastEditTimestamp.name, currentTimestamp)
            )
            val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
            val result = notes.findOneAndUpdate(filter, update, updateOptions)
            if (result != null) {
                notesCache.updateSavedNoteOrAdd(decryptNote(result))
                return true
            } else return false
        } else throw NoPermissionForEditException()
    }

    override suspend fun updateNoteDescription(noteId: String, editorUserId: String, newDescription: String?): Boolean {
        val note = getNoteById(noteId, editorUserId)
        if (note.creatorId == editorUserId) {
            val filter = Filters.eq("_id", noteId) // only owner can update description
            val encryptionKey = AESEncryptor.decrypt(note.encryptionKey)
            val newEncryptedDescription = if (newDescription!= null) {
                AESEncryptor.encrypt(newDescription, encryptionKey)
            } else null
            val currentTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            val update = Updates.combine(
                Updates.set(NoteModel::description.name, newEncryptedDescription),
                Updates.set(NoteModel::lastEditTimestamp.name, currentTimestamp)
            )
            val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
            val result = notes.findOneAndUpdate(filter, update, updateOptions)
            if (result != null) {
                notesCache.updateSavedNoteOrAdd(decryptNote(result))
                return true
            } else return false
        } else throw NoPermissionForEditException()
    }

    // TODO: Add partially updated notes from NotesRoom
    override suspend fun updateNoteText(noteId: String, editorUserId: String, newText: String): Boolean {
        val note = getNoteById(noteId, editorUserId)
        if (isUserCanEdit(note, editorUserId)) {
            val filter = Filters.eq("_id", noteId)
            val encryptionKey = AESEncryptor.decrypt(note.encryptionKey)
            val newEncryptedText = AESEncryptor.encrypt(newText, encryptionKey)
            val currentTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            val update = Updates.combine(
                Updates.set(NoteModel::title.name, newEncryptedText),
                Updates.set(NoteModel::lastEditTimestamp.name, currentTimestamp)
            )
            val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
            val result = notes.findOneAndUpdate(filter, update, updateOptions)
            if (result != null) {
                notesCache.updateSavedNoteOrAdd(decryptNote(result))
                return true
            } else return false
        } else throw NoPermissionForEditException()
    }

    // note sharing

    override suspend fun enableNoteSharing(noteId: String, requestedUserId: String): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", noteId),
            Filters.eq(NoteModel::creatorId.name, requestedUserId),
            Filters.eq(NoteModel::isSharing.name, false)
        )
        val update = Updates.set(NoteModel::isSharing.name, true)
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = notes.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            notesCache.updateSavedNote(decryptNote(result))
            return true
        } else return false
    }

    override suspend fun disableNoteSharing(noteId: String, requestedUserId: String): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", noteId),
            Filters.eq(NoteModel::creatorId.name, requestedUserId),
            Filters.eq(NoteModel::isSharing.name, true)
        )
        val update = Updates.combine(
            Updates.set(NoteModel::isSharing.name, false),
            Updates.set(NoteModel::sharedEditorUserIds.name, emptyList<String>()),
            Updates.set(NoteModel::sharedReaderUserIds.name, emptyList<String>())
        )
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = notes.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            notesCache.updateSavedNote(decryptNote(result))
            return true
        } else return false
    }

    override suspend fun addUserToSharedEditorIds(noteId: String, requestedUserId: String, userId: String): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", noteId),
            Filters.eq(NoteModel::creatorId.name, requestedUserId),
            Filters.eq(NoteModel::isSharing.name, true)
        )
        val update = Updates.addToSet(NoteModel::sharedEditorUserIds.name, userId)
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = notes.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            notesCache.updateSavedNote(decryptNote(result))
            return true
        } else return false
    }

    override suspend fun addUsersToSharedEditorIds(
        noteId: String,
        requestedUserId: String,
        userIds: Set<String>
    ): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", noteId),
            Filters.eq(NoteModel::creatorId.name, requestedUserId),
            Filters.eq(NoteModel::isSharing.name, true)
        )
        val update = Updates.addEachToSet(NoteModel::sharedEditorUserIds.name, userIds.toList())
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = notes.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            notesCache.updateSavedNote(decryptNote(result))
            return true
        } else return false
    }

    override suspend fun removeUserFromSharedEditorIds(noteId: String, requestedUserId: String, userId: String): Boolean {
        val filter = if (requestedUserId == userId) {
            Filters.and(
                Filters.eq("_id", noteId),
                Filters.eq(NoteModel::isSharing.name, true)
            )
        } else {
            Filters.and(
                Filters.eq("_id", noteId),
                Filters.eq(NoteModel::creatorId.name, requestedUserId),
                Filters.eq(NoteModel::isSharing.name, true)
            )
        }
        val update = Updates.pull(NoteModel::sharedEditorUserIds.name, userId)
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = notes.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            notesCache.updateSavedNote(decryptNote(result))
            return true
        } else return false
    }

    override suspend fun removeUsersFromSharedEditorIds(
        noteId: String,
        requestedUserId: String,
        userIds: Set<String>
    ): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", noteId),
            Filters.eq(NoteModel::creatorId.name, requestedUserId),
            Filters.eq(NoteModel::isSharing.name, true)
        )
        val update = Updates.pullAll(NoteModel::sharedEditorUserIds.name, userIds.toList())
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = notes.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            notesCache.updateSavedNote(decryptNote(result))
            return true
        } else return false
    }

    override suspend fun addUserToSharedReaderIds(noteId: String, requestedUserId: String, userId: String): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", noteId),
            Filters.eq(NoteModel::creatorId.name, requestedUserId),
            Filters.eq(NoteModel::isSharing.name, true)
        )
        val update = Updates.addToSet(NoteModel::sharedReaderUserIds.name, userId)
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = notes.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            notesCache.updateSavedNote(decryptNote(result))
            return true
        } else return false
    }

    override suspend fun addUsersToSharedReaderIds(
        noteId: String,
        requestedUserId: String,
        userIds: Set<String>
    ): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", noteId),
            Filters.eq(NoteModel::creatorId.name, requestedUserId),
            Filters.eq(NoteModel::isSharing.name, true)
        )
        val update = Updates.addEachToSet(NoteModel::sharedReaderUserIds.name, userIds.toList())
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = notes.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            notesCache.updateSavedNote(decryptNote(result))
            return true
        } else return false
    }

    override suspend fun removeUserFromSharedReaderIds(
        noteId: String,
        requestedUserId: String,
        userId: String
    ): Boolean {
        val filter = if (requestedUserId == userId) {
            Filters.and(
                Filters.eq("_id", noteId),
                Filters.eq(NoteModel::isSharing.name, true)
            )
        } else {
            Filters.and(
                Filters.eq("_id", noteId),
                Filters.eq(NoteModel::creatorId.name, requestedUserId),
                Filters.eq(NoteModel::isSharing.name, true)
            )
        }
        val update = Updates.pull(NoteModel::sharedReaderUserIds.name, userId)
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = notes.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            notesCache.updateSavedNote(decryptNote(result))
            return true
        } else return false
    }

    override suspend fun removeUsersFromSharedReaderIds(
        noteId: String,
        requestedUserId: String,
        userIds: Set<String>
    ): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", noteId),
            Filters.eq(NoteModel::creatorId.name, requestedUserId),
            Filters.eq(NoteModel::isSharing.name, true)
        )
        val update = Updates.pullAll(NoteModel::sharedReaderUserIds.name, userIds.toList())
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = notes.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            notesCache.updateSavedNote(decryptNote(result))
            return true
        } else return false
    }

    override suspend fun removeAllUsersFromSharedNote(noteId: String, requestedUserId: String): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", noteId),
            Filters.eq(NoteModel::creatorId.name, requestedUserId)
        )
        val update = Updates.combine(
            Updates.set(NoteModel::sharedReaderUserIds.name, emptyList<String>()),
            Updates.set(NoteModel::sharedEditorUserIds.name, emptyList<String>())
        )
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = notes.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            notesCache.updateSavedNote(result)
            return true
        } else return false
    }

    override suspend fun removeUserFromAllSharedNotes(userId: String): Boolean {
        val filter = Filters.or(
            Filters.`in`(NoteModel::sharedEditorUserIds.name, userId),
            Filters.`in`(NoteModel::sharedReaderUserIds.name, userId)
        )
        val update = Updates.combine(
            Updates.pull(NoteModel::sharedEditorUserIds.name, userId),
            Updates.pull(NoteModel::sharedReaderUserIds.name, userId)
        )
        val notesToUpdate = notes.find(filter).filter { notesCache.isNoteSaved(it.id) }.map {
            val newNote = it.copy(
                sharedEditorUserIds = if (it.sharedEditorUserIds.contains(userId)) {
                    it.sharedEditorUserIds.toMutableSet().apply {
                        remove(userId)
                    }
                } else it.sharedEditorUserIds,
                sharedReaderUserIds = if (it.sharedReaderUserIds.contains(userId)) {
                    it.sharedReaderUserIds.toMutableSet().apply {
                        remove(userId)
                    }
                } else it.sharedReaderUserIds
            )
            decryptNote(newNote)
        }.toList()
        notesCache.updateSavedNotesIfExists(notesToUpdate)
        val result = notes.updateMany(filter, update)
        return result.modifiedCount != 0L
    }

    // user roles

    override suspend fun updateNoteOwner(noteId: String, requestedUserId: String, userId: String): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", noteId),
            Filters.eq(NoteModel::isSharing.name, true),
            Filters.eq(NoteModel::creatorId.name, requestedUserId)
        )
        val update = Updates.combine(
            Updates.set(NoteModel::creatorId.name, userId),
            Updates.pull(NoteModel::sharedEditorUserIds.name, userId),
            Updates.pull(NoteModel::sharedReaderUserIds.name, userId),
            Updates.addToSet(NoteModel::sharedEditorUserIds.name, requestedUserId)
        )
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = notes.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            notesCache.updateSavedNote(decryptNote(result))
            return true
        } else return false
    }

    override suspend fun moveUserToReaders(noteId: String, requestedUserId: String, userId: String): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", noteId),
            Filters.eq(NoteModel::isSharing.name, true),
            Filters.eq(NoteModel::creatorId.name, requestedUserId)
        )
        val update = Updates.combine(
            Updates.pull(NoteModel::sharedEditorUserIds.name, userId),
            Updates.addToSet(NoteModel::sharedReaderUserIds.name, userId)
        )
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = notes.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            notesCache.updateSavedNote(decryptNote(result))
            return true
        } else return false
    }

    override suspend fun moveUsersToReaders(noteId: String, requestedUserId: String, userIds: Set<String>): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", noteId),
            Filters.eq(NoteModel::isSharing.name, true),
            Filters.eq(NoteModel::creatorId.name, requestedUserId)
        )
        val update = Updates.combine(
            Updates.pullAll(NoteModel::sharedEditorUserIds.name, userIds.toList()),
            Updates.addEachToSet(NoteModel::sharedReaderUserIds.name, userIds.toList())
        )
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = notes.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            notesCache.updateSavedNote(decryptNote(result))
            return true
        } else return false
    }

    override suspend fun moveUserToEditors(noteId: String, requestedUserId: String, userId: String): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", noteId),
            Filters.eq(NoteModel::isSharing.name, true),
            Filters.eq(NoteModel::creatorId.name, requestedUserId)
        )
        val update = Updates.combine(
            Updates.pull(NoteModel::sharedReaderUserIds.name, userId),
            Updates.addToSet(NoteModel::sharedEditorUserIds.name, userId)
        )
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = notes.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            notesCache.updateSavedNote(decryptNote(result))
            return true
        } else return false
    }

    override suspend fun moveUsersToEditors(noteId: String, requestedUserId: String, userIds: Set<String>): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", noteId),
            Filters.eq(NoteModel::isSharing.name, true),
            Filters.eq(NoteModel::creatorId.name, requestedUserId)
        )
        val update = Updates.combine(
            Updates.pullAll(NoteModel::sharedReaderUserIds.name, userIds.toList()),
            Updates.addEachToSet(NoteModel::sharedEditorUserIds.name, userIds.toList())
        )
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = notes.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            notesCache.updateSavedNote(decryptNote(result))
            return true
        } else return false
    }

    override suspend fun isNoteReadableForUser(noteId: String, userId: String): Boolean {
        try {
            getNoteById(noteId, userId)
            return true
        } catch (e: NoteNotFoundException) {
            return false
        }
    }

    override suspend fun isNoteEditableForUser(noteId: String, userId: String): Boolean {
        try {
            val note = getNoteById(noteId, userId)
            return isUserCanEdit(note, userId)
        } catch (e: NoteNotFoundException) {
            return false
        }
    }

    override suspend fun deleteNoteById(noteId: String, requestedUserId: String): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", noteId),
            Filters.eq(NoteModel::creatorId.name, requestedUserId)
        )
        val result = notes.findOneAndDelete(filter)
        if (result != null) notesCache.deleteNoteById(result.id)
        return result != null
    }

    override suspend fun deleteNotesById(noteIds: Set<String>, requestedUserId: String): Boolean {
        val filter = Filters.and(
            Filters.`in`("_id", noteIds),
            Filters.eq(NoteModel::creatorId.name, requestedUserId)
        )
        val noteIdsToDelete = notes.find(filter).map { it.id }.toList()
        notesCache.deleteNotesByIds(noteIdsToDelete)
        val result = notes.deleteMany(filter)
        return result.deletedCount != 0L
    }

    override suspend fun deleteNoteForUser(userId: String, noteId: String): Boolean {
        val createdNoteFilter = Filters.and(
            Filters.eq("_id", noteId),
            Filters.eq(NoteModel::creatorId.name, userId)
        )
        val createdNoteToDelete = notes.find(createdNoteFilter).singleOrNull()
        if (createdNoteToDelete != null) {
            notesCache.deleteNoteById(createdNoteToDelete.id)
            return notes.deleteOne(createdNoteFilter)
                .deletedCount != 0L
        } else {
            val sharedNoteFilter = Filters.and(
                Filters.eq("_id", noteId),
                Filters.ne(NoteModel::creatorId.name, userId)
            )
            val sharedNoteUpdate = Updates.combine(
                Updates.pull(NoteModel::sharedEditorUserIds.name, userId),
                Updates.pull(NoteModel::sharedReaderUserIds.name, userId)
            )
            val sharedNoteUpdateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
            val sharedNoteResult = notes.findOneAndUpdate(sharedNoteFilter, sharedNoteUpdate, sharedNoteUpdateOptions)
            if (sharedNoteResult != null) {
                notesCache.updateSavedNote(sharedNoteResult)
                return true
            } else return false
        }
    }

    override suspend fun deleteNotesForUser(userId: String, noteIds: Set<String>): Boolean {
        val createdNotesFilter = Filters.and(
            Filters.`in`("_id", noteIds.toList()),
            Filters.eq(NoteModel::creatorId.name, userId)
        )
        val sharedNotesFilter = Filters.and(
            Filters.`in`("_id", noteIds.toList()),
            Filters.ne(NoteModel::creatorId.name, userId)
        )
        val sharedNotesUpdate = Updates.combine(
            Updates.pull(NoteModel::sharedEditorUserIds.name, userId),
            Updates.pull(NoteModel::sharedReaderUserIds.name, userId)
        )
        val createdNotesIdsToDelete = notes.find(createdNotesFilter).map { it.id }.toList()
        notesCache.deleteNotesByIds(createdNotesIdsToDelete)
        val sharedNotesToUpdate = notes.find(sharedNotesFilter).filter{ notesCache.isNoteSaved(it.id) }.map {
            val newNote = it.copy(
                sharedEditorUserIds = if (it.sharedEditorUserIds.contains(userId)) {
                    it.sharedEditorUserIds.toMutableSet().apply {
                        remove(userId)
                    }
                } else it.sharedEditorUserIds,
                sharedReaderUserIds = if (it.sharedReaderUserIds.contains(userId)) {
                    it.sharedReaderUserIds.toMutableSet().apply {
                        remove(userId)
                    }
                } else it.sharedReaderUserIds
            )
            decryptNote(newNote)
        }.toList()
        notesCache.updateSavedNotesIfExists(sharedNotesToUpdate)
        val createdNotesResult = notes.deleteMany(createdNotesFilter)
            .deletedCount != 0L
        val sharedNotesResult = notes.updateMany(sharedNotesFilter, sharedNotesUpdate)
            .modifiedCount != 0L
        return createdNotesResult || sharedNotesResult
    }

    override suspend fun deleteAllUserCreatedNotes(userId: String): Boolean {
        val userCreatedNotesFilter = Filters.eq(NoteModel::creatorId.name, userId)
        val notesIdsToDelete = notes.find(userCreatedNotesFilter).map { it.id }.toList()
        notesCache.deleteNotesByIds(notesIdsToDelete)
        val resultForUserCreatedNotes = notes.deleteMany(userCreatedNotesFilter)
            .deletedCount != 0L
        return resultForUserCreatedNotes
    }

    override suspend fun deleteAllNotesForUser(userId: String): Boolean {
        val sharedNotesResult = removeUserFromAllSharedNotes(userId)
        val createdNotesResult = deleteAllUserCreatedNotes(userId)
        return sharedNotesResult || createdNotesResult
    }

    // UTILS

    private fun isUserCanRead(note: NoteModel, userId: String): Boolean {
        return if (note.isSharing) {
            note.creatorId == userId
                    || note.sharedEditorUserIds.contains(userId)
                    || note.sharedReaderUserIds.contains(userId)
        } else note.creatorId == userId
    }

    private fun isUserCanEdit(note: NoteModel, userId: String): Boolean {
        return if (note.isSharing) {
            note.creatorId == userId || note.sharedEditorUserIds.contains(userId)
        } else note.creatorId == userId
    }

    private fun encryptNote(note: NoteModel, encryptionKey: String): NoteModel {
        return note.copy(
            title = note.title?.run { AESEncryptor.encrypt(this, secretKey = encryptionKey) },
            description = note.description?.run { AESEncryptor.encrypt(this, secretKey = encryptionKey) },
            text = AESEncryptor.decrypt(note.text, secretKey = encryptionKey)
        )
    }

    private fun decryptNote(note: NoteModel): NoteModel {
        val decryptionKey = AESEncryptor.decrypt(note.encryptionKey)
        return note.copy(
            title = note.title?.run { AESEncryptor.decrypt(this, secretKey = decryptionKey) },
            description = note.description?.run { AESEncryptor.decrypt(this, secretKey = decryptionKey) },
            text = AESEncryptor.decrypt(note.text, secretKey = decryptionKey),
            encryptionKey = decryptionKey
        )
    }
}