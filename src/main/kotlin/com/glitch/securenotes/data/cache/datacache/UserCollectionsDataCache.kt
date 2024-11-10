package com.glitch.securenotes.data.cache.datacache

import com.glitch.securenotes.data.model.entity.UserCollectionModel

interface UserCollectionsDataCache {

    // GET

    fun isCollectionsForUserSaved(userId: String): Boolean

    fun isCollectionInfoSaved(userId: String, collectionId: String): Boolean

    fun getCollectionById(collectionId: String, userId: String): UserCollectionModel?

    fun getCollectionsForUser(userId: String): List<UserCollectionModel>?

    // ADD

    fun saveCollectionsToCache(userId: String, collections: List<UserCollectionModel>)

    fun addCollectionForUser(userId: String, collection: UserCollectionModel)

    // UPDATE

    fun updateCollection(userId: String, collection: UserCollectionModel)

    // DELETE

    fun deleteCollectionById(userId: String, collectionId: String)

    fun deleteCollectionsByIds(userId: String, collectionIds: List<String>)

    fun deleteCollectionsForUser(userId: String)

}