package com.glitch.securenotes.data.cache.cachedmodel

import com.glitch.securenotes.data.model.entity.ResourceModel
import io.ktor.util.collections.*

data class CachedNoteResourceModel(
    // map of resourceId to ResourceModel
    val noteResources: ConcurrentMap<String, ResourceModel>,
    val lastUsedTimestamp: Long
)
