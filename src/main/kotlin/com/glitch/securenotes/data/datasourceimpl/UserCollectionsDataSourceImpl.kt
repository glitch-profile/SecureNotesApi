package com.glitch.securenotes.data.datasourceimpl

import com.glitch.floweryapi.domain.utils.encryptor.AESEncryptor
import com.glitch.securenotes.data.cache.datacache.UserCollectionsDataCache
import com.glitch.securenotes.data.datasource.UserCollectionsDataSource
import com.glitch.securenotes.data.exceptions.usercollections.CollectionNotFoundException
import com.glitch.securenotes.data.model.entity.UserCollectionModel
import com.mongodb.client.model.*
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList

// TODO: Implement cache system by user ids
class UserCollectionsDataSourceImpl(
    db: MongoDatabase,
    private val cache: UserCollectionsDataCache
): UserCollectionsDataSource {

    private val collections = db.getCollection<UserCollectionModel>("UserCollections")

    // GET

    // TODO: Rework this method
    override suspend fun getCollectionById(collectionId: String, userId: String): UserCollectionModel {
        if (cache.isCollectionInfoSaved(userId, collectionId)) {
            return cache.getCollectionById(collectionId, userId)!!
        } else {
//            val filter = Filters.and(
//                Filters.eq("_id", collectionId),
//                Filters.eq(UserCollectionModel::userId.name, userId)
//            )
//            val result = collections.find(filter).singleOrNull() ?: throw CollectionNotFoundException()
//            return decryptCollection(result)
            val foundedCollections = getCollectionForUser(userId)
            return foundedCollections.firstOrNull { it.id == collectionId } ?: throw CollectionNotFoundException()
        }
    }

    override suspend fun getCollectionsByIds(collectionsIds: List<String>, userId: String): List<UserCollectionModel> {
        val foundedCollections = getCollectionForUser(userId)
        return foundedCollections.filter { collectionsIds.contains(it.id) }
    }

    override suspend fun getCollectionForUser(userId: String): List<UserCollectionModel> {
        if (cache.isCollectionsForUserSaved(userId)) {
            return cache.getCollectionsForUser(userId)!!
        } else {
            val filter = Filters.eq(UserCollectionModel::userId.name, userId)
            val result = collections.find(filter)
                .sort(Sorts.ascending(UserCollectionModel::title.name))
                .toList()
            val decryptedCollections = result.map { decryptCollection(it) }
            cache.saveCollectionsToCache(userId, decryptedCollections)
            return decryptedCollections
        }
    }

    // CREATE

    override suspend fun addCollection(title: String, description: String, userId: String): UserCollectionModel {
        val collection = UserCollectionModel(
            title = title,
            description = description,
            userId = userId
        )
        val encryptedCollection = encryptCollection(collection)
        cache.addCollectionForUser(userId, collection)
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
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = collections.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            cache.updateCollection(userId, result)
        }
        return result != null
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
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = collections.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            cache.updateCollection(userId, result)
        }
        return result != null
    }

    override suspend fun addNoteToCollection(collectionId: String, userId: String, noteId: String): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", collectionId),
            Filters.eq(UserCollectionModel::userId.name, userId)
        )
        val update = Updates.addToSet(UserCollectionModel::assignedNotes.name, noteId)
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = collections.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            cache.updateCollection(userId, result)
        }
        return result != null
    }

    override suspend fun addNotesToCollection(collectionId: String, userId: String, noteIds: List<String>): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", collectionId),
            Filters.eq(UserCollectionModel::userId.name, userId)
        )
        val update = Updates.addEachToSet(UserCollectionModel::assignedNotes.name, noteIds)
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = collections.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            cache.updateCollection(userId, result)
        }
        return result != null
    }

    override suspend fun removeNoteFromCollection(collectionId: String, userId: String, noteId: String): Boolean {
        val filter = Filters.and(
            Filters.eq("_id", collectionId),
            Filters.eq(UserCollectionModel::userId.name, userId)
        )
        val update = Updates.pull(UserCollectionModel::assignedNotes.name, noteId)
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = collections.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            cache.updateCollection(userId, result)
        }
        return result != null
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
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val result = collections.findOneAndUpdate(filter, update, updateOptions)
        if (result != null) {
            cache.updateCollection(userId, result)
        }
        return result != null
    }

    // DELETE

    override suspend fun deleteCollectionById(collectionId: String, userId: String): Boolean {
        cache.deleteCollectionById(userId, collectionId)
        val filter = Filters.and(
            Filters.eq("_id", collectionId),
            Filters.eq(UserCollectionModel::userId.name, userId)
        )
        val result = collections.deleteOne(filter)
        return result.deletedCount != 0L
    }

    override suspend fun deleteCollectionByIds(collectionIds: List<String>, userId: String): Boolean {
        cache.deleteCollectionsByIds(userId, collectionIds)
        val filter = Filters.and(
            Filters.`in`("_id", collectionIds),
            Filters.eq(UserCollectionModel::userId.name, userId)
        )
        val result = collections.deleteMany(filter)
        return result.deletedCount != 0L
    }

    override suspend fun deleteCollectionsForUser(userId: String): Boolean {
        cache.deleteCollectionsForUser(userId)
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