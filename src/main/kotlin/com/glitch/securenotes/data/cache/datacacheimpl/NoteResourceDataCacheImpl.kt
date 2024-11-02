package com.glitch.securenotes.data.cache.datacacheimpl

import com.glitch.securenotes.data.cache.cachedmodel.CachedNoteResourceModel
import com.glitch.securenotes.data.cache.datacache.NoteResourcesDataCache
import com.glitch.securenotes.data.model.entity.ResourceModel
import io.ktor.util.collections.*
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

class NoteResourceDataCacheImpl(
    private val maxCacheSize: Int
): NoteResourcesDataCache {

    private val resources = ConcurrentHashMap<String, CachedNoteResourceModel>()

    override fun getResourceById(noteId: String, resourceId: String): ResourceModel? {
        val resourceInfo = resources[noteId]
        if (resourceInfo != null) {
            resources[noteId] = resourceInfo.copy(
                lastUsedTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            )
            return resourceInfo.noteResources[resourceId]
        } else return null
    }

    override fun getResourcesByIds(noteId: String, resourceIds: List<String>): List<ResourceModel>? {
        if (isNoteKeyExists(noteId)) {
            val resourceInfo = resources[noteId]!!
            val foundedResource = mutableListOf<ResourceModel>()
            resourceIds.forEach {
                if (resourceInfo.noteResources.containsKey(it))
                    foundedResource.add(resourceInfo.noteResources[it]!!)
            }
            resources[noteId] = resourceInfo.copy(
                lastUsedTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            )
            return foundedResource.sortedByDescending { it.lastEditTimestamp }
        } else return null
    }

    override fun getResourcesForNote(noteId: String): List<ResourceModel>? {
        if (isNoteKeyExists(noteId)) {
            val noteResources = resources[noteId]!!
            val resourceList = noteResources.noteResources.values.toList()
            resources[noteId] = noteResources.copy(
                lastUsedTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            )
            return resourceList.sortedByDescending { it.lastEditTimestamp }
        } else return null
    }

    override fun isNoteKeyExists(noteId: String): Boolean {
        return resources.containsKey(noteId)
    }

    override fun isResourceForNoteSaved(noteId: String, resourceId: String): Boolean {
        return resources[noteId]?.noteResources?.containsKey(resourceId) ?: false
    }

    override fun saveResourcesToCache(noteId: String, resourceList: List<ResourceModel>) {
        if (!isNoteKeyExists(noteId)) {
            if (resources.size >= maxCacheSize) {
                val oldestResource = resources.minByOrNull { it.value.lastUsedTimestamp }!!
                deleteNoteFromCache(oldestResource.key)
            }
            val resourceMap = ConcurrentMap<String, ResourceModel>()
            resourceList.forEach { resourceMap[it.id] = it }
            resources[noteId] = CachedNoteResourceModel(
                noteResources = resourceMap,
                lastUsedTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            )
        }
    }

    override fun addResourceToNote(noteId: String, resource: ResourceModel) {
        if (isNoteKeyExists(noteId)) {
            val resourceInfo = resources[noteId]!!
            resourceInfo.noteResources[resource.id] = resource
            resources[noteId] = resourceInfo.copy(
                lastUsedTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            )
        }
    }

    override fun updateResourceForNote(noteId: String, resource: ResourceModel) {
        if (isNoteKeyExists(noteId)) {
            val resourceInfo = resources[noteId]!!
            if (resourceInfo.noteResources.containsKey(resource.id)) {
                resourceInfo.noteResources[resource.id] = resource
                resources[noteId] = resourceInfo.copy(
                    lastUsedTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
                )
            }
        }
    }

    override fun deleteResourceFromNote(noteId: String, resourceId: String) {
        if (isNoteKeyExists(noteId)) {
            val resourceInfo = resources[noteId]!!
            resourceInfo.noteResources.remove(resourceId)
            resources[noteId] = resourceInfo.copy(
                lastUsedTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            )
        }
    }

    override fun deleteResourcesFromNote(noteId: String, resourceIds: List<String>) {
        if (isNoteKeyExists(noteId)) {
            val resourceInfo = resources[noteId]!!
            resourceIds.forEach { resourceInfo.noteResources.remove(it) }
            resources[noteId] = resourceInfo.copy(
                lastUsedTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            )
        }
    }

    override fun deleteNoteFromCache(noteId: String) {
        resources.remove(noteId)
    }
}