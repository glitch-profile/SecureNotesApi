package com.glitch.securenotes.data.cache.cachedmodel

import com.glitch.securenotes.data.model.entity.UserCollectionModel
import io.ktor.util.collections.*

data class CachedCollectionsModel(
    // map of collection id to collection info
    val collection: ConcurrentMap<String, UserCollectionModel>,
    val lastUsedTimestamp: Long
)