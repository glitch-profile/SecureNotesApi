package com.glitch.securenotes.data.cache.datacache

import com.glitch.securenotes.data.model.entity.ResourceModel

interface NoteResourcesDataCache {

    fun getResourceById(noteId: String, resourceId: String): ResourceModel?

    fun getResourcesByIds(noteId: String, resourceIds: List<String>): List<ResourceModel>?

    fun getResourcesForNote(noteId: String): List<ResourceModel>?

    fun isNoteKeyExists(noteId: String): Boolean

    fun isResourceForNoteSaved(noteId: String, resourceId: String): Boolean

    fun saveResourcesToCache(noteId: String, resourceList: List<ResourceModel>)

    fun addResourceToNote(noteId: String, resource: ResourceModel)

    fun updateResourceForNote(noteId: String, resource: ResourceModel)

    fun deleteResourceFromNote(noteId: String, resourceId: String)

    fun deleteResourcesFromNote(noteId: String, resourceIds: List<String>)

    fun deleteNoteFromCache(noteId: String)

}