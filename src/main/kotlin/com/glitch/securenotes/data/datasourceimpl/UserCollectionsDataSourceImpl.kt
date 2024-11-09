package com.glitch.securenotes.data.datasourceimpl

import com.glitch.floweryapi.domain.utils.encryptor.AESEncryptor
import com.glitch.securenotes.data.datasource.UserCollectionsDataSource
import com.glitch.securenotes.data.datasource.notes.NotesDataSource
import com.glitch.securenotes.data.exceptions.usercollections.CollectionNotFoundException
import com.glitch.securenotes.data.model.entity.UserCollectionModel
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList

// TODO: Implement cache system by user ids
class UserCollectionsDataSourceImpl(
    db: MongoDatabase
): UserCollectionsDataSource {

    private val collections = db.getCollection<UserCollectionModel>("UserCollections")

    // GET

    override suspend fun getCollectionById(collectionId: String, userId: String): UserCollectionModel {
        val filter = Filters.and(
            Filters.eq("_id", collectionId),
            Filters.eq(UserCollectionModel::userId.name, userId)
        )
        val result = collections.find(filter).singleOrNull() ?: throw CollectionNotFoundException()
        return decryptCollection(result)
    }

    override suspend fun getCollectionsByIds(collectionsIds: String, userId: String): List<UserCollectionModel> {
        val filter = Filters.and(
            Filters.`in`("_id", collectionsIds),
            Filters.eq(UserCollectionModel::userId.name, userId)
        )
        val result = collections.find(filter)
            .sort(Sorts.ascending(UserCollectionModel::title.name))
            .toList()
        val decryptedCollections = result.map { decryptCollection(it) }
        return decryptedCollections
    }

    override suspend fun getCollectionForUser(userId: String): List<UserCollectionModel> {
        val filter = Filters.eq(UserCollectionModel::userId.name, userId)
        val result = collections.find(filter)
            .sort(Sorts.ascending(UserCollectionModel::title.name))
            .toList()
        val decryptedCollections = result.map { decryptCollection(it) }
        return decryptedCollections
    }

    // CREATE

    override suspend fun addCollection(title: String, description: String, userId: String): UserCollectionModel {
        val collection = UserCollectionModel(
            title = title,
            description = description,
            userId = userId
        )
        val encryptedCollection = encryptCollection(collection)
        collections.insertOne(encryptedCollection)
        return collection
    }

    // EDIT

    override suspend fun updateCollectionTitle(collectionId: String, userId: String, newTitle: String): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", collectionId),
            Filters.eq(UserCollectionModel::userId.name)
        )
        val titleEncrypted = AESEncryptor.encrypt(newTitle)
        val update = Updates.set(UserCollectionModel::title.name, titleEncrypted)
        val result = collections.updateOne(filter, update)
        return result.modifiedCount != 0L
    }

    override suspend fun updateCollectionDescription(
        collectionId: String,
        userId: String,
        newDescription: String?
    ): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", collectionId),
            Filters.eq(UserCollectionModel::userId.name, userId)
        )
        val encryptedDescription = newDescription?.run { AESEncryptor.encrypt(this) }
        val update = Updates.set(UserCollectionModel::description.name, encryptedDescription)
        val result = collections.updateOne(filter, update)
        return result.modifiedCount != 0L
    }

    override suspend fun addNoteToCollection(collectionId: String, userId: String, noteId: String): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", collectionId),
            Filters.eq(UserCollectionModel::userId.name, userId)
        )
        val update = Updates.addToSet(UserCollectionModel::assignedNotes.name, noteId)
        val result = collections.updateOne(filter, update)
        return result.modifiedCount != 0L
    }

    override suspend fun addNotesToCollection(collectionId: String, userId: String, noteIds: List<String>): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", collectionId),
            Filters.eq(UserCollectionModel::userId.name, userId)
        )
        val update = Updates.addEachToSet(UserCollectionModel::assignedNotes.name, noteIds)
        val result = collections.updateOne(filter, update)
        return result.modifiedCount != 0L
    }

    override suspend fun removeNoteFromCollection(collectionId: String, userId: String, noteId: String): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", collectionId),
            Filters.eq(UserCollectionModel::userId.name, userId)
        )
        val update = Updates.pull(UserCollectionModel::assignedNotes.name, noteId)
        val result = collections.updateOne(filter, update)
        return result.modifiedCount != 0L
    }

    override suspend fun removeNotesFromCollection(
        collectionId: String,
        userId: String,
        noteIds: List<String>
    ): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", collectionId),
            Filters.eq(UserCollectionModel::userId.name, userId)
        )
        val update = Updates.pullAll(UserCollectionModel::assignedNotes.name, noteIds)
        val result = collections.updateOne(filter, update)
        return result.modifiedCount != 0L
    }

    // DELETE

    override suspend fun deleteCollectionById(collectionId: String, userId: String): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", collectionId),
            Filters.eq(UserCollectionModel::userId.name, userId)
        )
        val result = collections.deleteOne(filter)
        return result.deletedCount != 0L
    }

    override suspend fun deleteCollectionByIds(collectionIds: List<String>, userId: String): Boolean {
        val filter = Filters.and(
            Filters.`in`("_id", collectionIds),
            Filters.eq(UserCollectionModel::userId.name, userId)
        )
        val result = collections.deleteMany(filter)
        return result.deletedCount != 0L
    }

    override suspend fun deleteCollectionsForUser(userId: String): Boolean {
        val filter = Filters.eq(UserCollectionModel::userId.name, userId)
        val result = collections.deleteMany(filter)
        return result.deletedCount != 0L
    }

    // UTIL

    private fun encryptCollection(collection: UserCollectionModel): UserCollectionModel {
        return collection.copy(
            title = AESEncryptor.encrypt(collection.title),
            description = collection.description?.run { AESEncryptor.encrypt(this) }
        )
    }

    private fun decryptCollection(collection: UserCollectionModel): UserCollectionModel {
        return collection.copy(
            title = AESEncryptor.decrypt(collection.title),
            description = collection.description?.run { AESEncryptor.decrypt(this) }
        )
    }
}