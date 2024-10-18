package com.glitch.securenotes.data.datasourceimpl

import com.glitch.floweryapi.domain.utils.encryptor.AESEncryptor
import com.glitch.securenotes.data.datasource.NotesDataSource
import com.glitch.securenotes.data.exceptions.notes.NoPermissionForEditException
import com.glitch.securenotes.data.exceptions.notes.NoteNotFoundException
import com.glitch.securenotes.data.model.entity.NoteModel
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.bson.conversions.Bson
import java.time.OffsetDateTime
import java.time.ZoneId

class NotesDataSourceImpl(
    db: MongoDatabase
): NotesDataSource {

    private val notes = db.getCollection<NoteModel>("UserNotes")

    override suspend fun getNotesForUser(
        userId: String,
        excludedNotesId: List<String>,
        page: Int,
        limit: Int
    ): List<NoteModel> {
        val filter = Filters.and(
            Filters.not(Filters.`in`("_id", excludedNotesId)),
            Filters.or(
                Filters.eq(NoteModel::creatorId.name, userId),
                Filters.`in`(NoteModel::sharedEditorsUsersIds.name, userId)
            )
        )
        if (limit == -1) {
            val result = notes.find(filter)
                .sort(Sorts.descending(NoteModel::lastEditTimestamp.name))
                .toList()
            return result
        } else if (limit < 0 || page < 0) {
            throw IllegalArgumentException()
        } else {
            val result = notes.find(filter)
                .sort(Sorts.descending(NoteModel::lastEditTimestamp.name))
                .skip(page * limit)
                .limit(limit)
                .toList()
            return result
        }
    }

    override suspend fun getProtectedNotesForUser(
        userId: String,
        includedNotesIds: List<String>,
        page: Int,
        limit: Int
    ): List<NoteModel> {
        val filter = Filters.and(
            Filters.`in`("_id", includedNotesIds),
            Filters.or(
                Filters.eq(NoteModel::creatorId.name, userId),
                Filters.`in`(NoteModel::sharedEditorsUsersIds.name, userId)
            )
        )
        if (limit == -1) {
            val result = notes.find(filter)
                .sort(Sorts.descending(NoteModel::lastEditTimestamp.name))
                .toList()
            return result
        } else if (limit < 0 || page < 0) {
            throw IllegalArgumentException()
        } else {
            val result = notes.find(filter)
                .sort(Sorts.descending(NoteModel::lastEditTimestamp.name))
                .skip(page * limit)
                .limit(limit)
                .toList()
            return result
        }
    }

    override suspend fun getNotesForUser(
        userId: String,
        page: Int,
        limit: Int,
        onlyIncludedIds: List<String>,
        excludeIds: List<String>
    ): List<NoteModel> {
        val filters = mutableListOf<Bson>().apply {
            add(Filters.eq(NoteModel::creatorId.name, userId))
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
            return result
        } else if (limit < 0 || page < 0) {
            throw IllegalArgumentException()
        } else {
            val result = notes.find(searchFilters)
                .sort(Sorts.descending(NoteModel::lastEditTimestamp.name))
                .skip(page * limit)
                .limit(limit)
                .toList()
            return result
        }
    }

    override suspend fun getOneNoteById(noteId: String): NoteModel {
        val filter = Filters.eq("_id", noteId)
        val result = notes.find(filter).singleOrNull() ?: throw NoteNotFoundException()
        return result
    }

    override suspend fun getManyNotesById(noteIds: List<String>): List<NoteModel> {
        val filter = Filters.`in`("_id", noteIds)
        val result = notes.find(filter).toList()
        return result
    }

    // CREATE

    override suspend fun createNewNote(
        creatorId: String,
        title: String?,
        description: String?,
        text: String
    ): NoteModel {
        val encryptionKey = AESEncryptor.generateSecret()
        val protectedEncryptionKey = AESEncryptor.encrypt(encryptionKey)
        val noteData = NoteModel(
            creatorId = creatorId,
            title = title,
            description = description,
            text = text,
            encryptionKey = protectedEncryptionKey,
            noteResources = emptyList(),
        )
        val encryptedNote = encryptNote(noteData, encryptionKey = encryptionKey)
        notes.insertOne(encryptedNote)
        return noteData
    }

    override suspend fun createNewNote(
        creatorId: String,
        title: String?,
        description: String?,
        text: String,
        creationTimestamp: Long,
        lastEditTimestamp: Long?
    ): NoteModel {
        val encryptionKey = AESEncryptor.generateSecret()
        val protectedEncryptionKey = AESEncryptor.encrypt(encryptionKey)
        val noteData = NoteModel(
            creatorId = creatorId,
            title = title,
            description = description,
            text = text,
            creationTimestamp = creationTimestamp,
            lastEditTimestamp = lastEditTimestamp,
            encryptionKey = protectedEncryptionKey,
            noteResources = emptyList(),
        )
        val encryptedNote = encryptNote(noteData, encryptionKey = encryptionKey)
        notes.insertOne(encryptedNote)
        return noteData
    }

    // UPDATE

    // info
    override suspend fun updateNoteTitle(noteId: String, editorUserId: String, newTitle: String?): Boolean {
        val note = getOneNoteById(noteId)
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
            return notes.updateOne(filter, update)
                .modifiedCount != 0L
        } else throw NoPermissionForEditException()
    }

    override suspend fun updateNoteDescription(noteId: String, editorUserId: String, newDescription: String?): Boolean {
        val note = getOneNoteById(noteId)
        if (note.creatorId == editorUserId) {
            val filter = Filters.eq("_id", noteId) // only owner can update title
            val encryptionKey = AESEncryptor.decrypt(note.encryptionKey)
            val newEncryptedDescription = if (newDescription!= null) {
                AESEncryptor.encrypt(newDescription, encryptionKey)
            } else null
            val currentTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            val update = Updates.combine(
                Updates.set(NoteModel::description.name, newEncryptedDescription),
                Updates.set(NoteModel::lastEditTimestamp.name, currentTimestamp)
            )
            return notes.updateOne(filter, update)
                .modifiedCount != 0L
        } else throw NoPermissionForEditException()
    }

    override suspend fun updateNoteText(noteId: String, editorUserId: String, newText: String): Boolean {
        val note = getOneNoteById(noteId)
        if (note.creatorId == editorUserId || note.sharedEditorsUsersIds.contains(editorUserId)) {
            val filter = Filters.eq("_id", noteId)
            val encryptionKey = AESEncryptor.decrypt(note.encryptionKey)
            val newEncryptedText = AESEncryptor.encrypt(newText, encryptionKey)
            val currentTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            val update = Updates.combine(
                Updates.set(NoteModel::title.name, newEncryptedText),
                Updates.set(NoteModel::lastEditTimestamp.name, currentTimestamp)
            )
            return notes.updateOne(filter, update)
                .modifiedCount != 0L
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
        val result = notes.updateOne(filter, update)
        return result.modifiedCount != 0L
    }

    override suspend fun disableNoteSharing(noteId: String, requestedUserId: String): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", noteId),
            Filters.eq(NoteModel::creatorId.name, requestedUserId),
            Filters.eq(NoteModel::isSharing.name, true)
        )
        val update = Updates.combine(
            Updates.set(NoteModel::isSharing.name, false),
            Updates.set(NoteModel::sharedEditorsUsersIds.name, emptyList<String>())
        )
        val result = notes.updateOne(filter, update)
        return result.modifiedCount != 0L
    }

    override suspend fun addUserToSharedIds(noteId: String, userId: String): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", noteId),
            Filters.ne(NoteModel::isSharing.name, true)
        )
        val update = Updates.addToSet(NoteModel::sharedEditorsUsersIds.name, userId)
        val result = notes.updateOne(filter, update)
        return result.modifiedCount != 0L
    }

    override suspend fun removeUserFromSharedIds(noteId: String, userId: String): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", noteId),
            Filters.ne(NoteModel::isSharing.name, true)
        )
        val update = Updates.pull(NoteModel::sharedEditorsUsersIds.name, userId)
        val result = notes.updateOne(filter, update)
        return result.modifiedCount != 0L
    }

//    override suspend fun removeUserFromSharedIds(noteIds: List<String>, userId: String): Boolean {
//        val filter = Filters.and(
//            Filters.`in`("_id", noteIds),
//            Filters.ne(NoteModel::isSharing.name, null)
//        )
//        val update = Updates.pull(NoteModel::sharedEditorsUsersIds.name, userId)
//        val result = notes.updateMany(filter, update)
//        return result.modifiedCount != 0L
//    }

    override suspend fun removeUsersFromSharedIds(noteId: String, userIds: List<String>): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", noteId),
            Filters.ne(NoteModel::isSharing.name, null)
        )
        val update = Updates.pullAll(NoteModel::sharedEditorsUsersIds.name, userIds)
        val result = notes.updateOne(filter, update)
        return result.modifiedCount != 0L
    }

    override suspend fun removeUserFromAllSharedNotes(userId: String): Boolean {
        val filter = Filters.`in`(NoteModel::sharedEditorsUsersIds.name, userId)
        val update = Updates.pull(NoteModel::sharedEditorsUsersIds.name, userId)
        val result = notes.updateMany(filter, update)
        return result.modifiedCount != 0L
    }

    // DELETE

    // its caller responsibility to delete resource files from this notes
    override suspend fun deleteOneNoteById(noteId: String): Boolean {
        val filter = Filters.eq("_id", noteId)
        val result = notes.deleteOne(filter)
        return result.deletedCount != 0L
    }

    // its caller responsibility to delete resource files from this notes
    override suspend fun deleteManyNotesById(noteIds: List<String>): Boolean {
        val filter = Filters.`in`("_id", noteIds)
        val result = notes.deleteMany(filter)
        return result.deletedCount != 0L
    }

    // its caller responsibility to delete files from this note
    override suspend fun deleteNoteForUser(userId: String, noteId: String): Boolean {
        val createdNoteFilter = Filters.and(
            Filters.eq("_id", noteId),
            Filters.eq(NoteModel::creatorId.name, userId)
        )
        val sharedNoteFilter = Filters.and(
            Filters.eq("_id", noteId),
            Filters.ne(NoteModel::creatorId.name, userId)
        )
        val sharedNoteUpdate = Updates.pull(NoteModel::sharedEditorsUsersIds.name, userId)
        val sharedNoteResult = notes.updateOne(sharedNoteFilter, sharedNoteUpdate)
            .modifiedCount != 0L
        val createdNoteResult = notes.deleteOne(createdNoteFilter)
            .deletedCount != 0L
        return sharedNoteResult || createdNoteResult
    }

    // its caller responsibility to delete resource files from this notes
    override suspend fun deleteNotesForUser(userId: String, noteIds: String): Boolean {
        val createdNotesFilter = Filters.and(
            Filters.`in`("_id", noteIds),
            Filters.eq(NoteModel::creatorId.name, userId)
        )
        val sharedNotesFilter = Filters.and(
            Filters.`in`("_id", noteIds),
            Filters.ne(NoteModel::creatorId.name, userId)
        )
        val sharedNotesUpdate = Updates.pull(NoteModel::sharedEditorsUsersIds.name, userId)
        val createdNotesResult = notes.deleteMany(createdNotesFilter)
            .deletedCount != 0L
        val sharedNotesResult = notes.updateMany(sharedNotesFilter, sharedNotesUpdate)
            .modifiedCount != 0L
        return createdNotesResult || sharedNotesResult
    }

    override suspend fun deleteAllUserCreatedNotes(userId: String): Boolean {
        val userCreatedNotesFilter = Filters.eq(NoteModel::creatorId.name, userId)
        val resultForUserCreatedNotes = notes.deleteMany(userCreatedNotesFilter)
            .deletedCount != 0L
        return resultForUserCreatedNotes
    }

    // its caller responsibility to delete resource files from this notes
    override suspend fun deleteAllNotesForUser(userId: String): Boolean {
        val sharedNotesResult = removeUserFromAllSharedNotes(userId)
        val createdNotesResult = deleteAllUserCreatedNotes(userId)
        return sharedNotesResult || createdNotesResult
    }

    // UTILS

    override fun encryptNote(note: NoteModel, encryptionKey: String): NoteModel {
        return note.copy(
            title = note.title?.run { AESEncryptor.decrypt(this, secretKey = encryptionKey) },
            description = note.description?.run { AESEncryptor.decrypt(this, secretKey = encryptionKey) },
            text = AESEncryptor.decrypt(note.text, secretKey = encryptionKey)
        )
    }

    override fun decryptNote(note: NoteModel, decryptionKey: String): NoteModel {
        return note.copy(
            title = note.title?.run { AESEncryptor.decrypt(this, secretKey = decryptionKey) },
            description = note.description?.run { AESEncryptor.decrypt(this, secretKey = decryptionKey) },
            text = AESEncryptor.decrypt(note.text, secretKey = decryptionKey)
        )
    }
}