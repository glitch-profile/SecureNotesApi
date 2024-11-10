package com.glitch.securenotes.data.cache.datacacheimpl

import com.glitch.securenotes.data.cache.cachedmodel.CachedCollectionsModel
import com.glitch.securenotes.data.cache.datacache.UserCollectionsDataCache
import com.glitch.securenotes.data.model.entity.UserCollectionModel
import io.ktor.util.collections.*
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

class UserCollectionsDataCacheImpl(
    private val maxCacheSize: Int
): UserCollectionsDataCache {

    private val cache = ConcurrentHashMap<String, CachedCollectionsModel>()

    override fun isCollectionsForUserSaved(userId: String): Boolean {
        return cache.containsKey(userId)
    }

    override fun isCollectionInfoSaved(userId: String, collectionId: String): Boolean {
        return cache[userId]?.collections?.containsKey(collectionId) ?: false
    }

    override fun getCollectionById(collectionId: String, userId: String): UserCollectionModel? {
        if (isCollectionsForUserSaved(userId)) {
            val collectionInfo = cache[userId]!!
            cache[userId] = collectionInfo.copy(
                lastUsedTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            )
            return collectionInfo.collections[collectionId]
        } else return null
    }

    override fun getCollectionsForUser(userId: String): List<UserCollectionModel>? {
        if (isCollectionsForUserSaved(userId)) {
            val collectionsInfo = cache[userId]!!
            cache[userId] = collectionsInfo.copy(
                lastUsedTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            )
            return collectionsInfo.collections.values
                .toList()
                .sortedBy { it.title }
        } else return null
    }

    override fun saveCollectionsToCache(userId: String, collections: List<UserCollectionModel>) {
        if (!isCollectionsForUserSaved(userId)) {
            if (cache.size >= maxCacheSize) {
                val collectionsInfoToDelete = cache.minByOrNull { it.value.lastUsedTimestamp }!!
                cache.remove(collectionsInfoToDelete.key)
            }
            val collectionsMap = ConcurrentMap<String, UserCollectionModel>()
            collections.forEach { collectionsMap[it.id] = it }
            cache[userId] = CachedCollectionsModel(
                collections = collectionsMap,
                lastUsedTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            )
        }
    }

    override fun addCollectionForUser(userId: String, collection: UserCollectionModel) {
        if (isCollectionsForUserSaved(userId)) {
            val collectionsInfo = cache[userId]!!
            collectionsInfo.collections[collection.id] = collection
            cache[userId] = collectionsInfo.copy(
                lastUsedTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            )
        }
    }

    override fun updateCollection(userId: String, collection: UserCollectionModel) {
        if (isCollectionsForUserSaved(userId)) {
            val collectionsInfo = cache[userId]!!
            if (collectionsInfo.collections.containsKey(collection.id)) {
                collectionsInfo.collections[collection.id] = collection
                cache[userId] = collectionsInfo.copy(
                    lastUsedTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
                )
            }
        }
    }

    override fun deleteCollectionById(userId: String, collectionId: String) {
        if (isCollectionsForUserSaved(userId)) {
            val collectionsInfo = cache[userId]!!
            collectionsInfo.collections.remove(collectionId)
            cache[userId] = collectionsInfo.copy(
                lastUsedTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            )
        }
    }

    override fun deleteCollectionsByIds(userId: String, collectionIds: List<String>) {
        if (isCollectionsForUserSaved(userId)) {
            val collectionsInfo = cache[userId]!!
            collectionIds.forEach { collectionsInfo.collections.remove(it) }
            cache[userId] = collectionsInfo.copy(
                lastUsedTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            )
        }
    }

    override fun deleteCollectionsForUser(userId: String) {
        cache.remove(userId)
    }
}