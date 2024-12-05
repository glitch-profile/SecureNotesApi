package com.glitch.securenotes.data.datasource

import com.glitch.securenotes.data.model.entity.UserCollectionModel

interface UserCollectionsDataSource {

    // GET

    suspend fun getCollectionById(collectionId: String, userId: String): UserCollectionModel

    suspend fun getCollectionsByIds(collectionsIds: Set<String>, userId: String): List<UserCollectionModel>

    suspend fun getCollectionForUser(userId: String): List<UserCollectionModel>

    // CREATE

    suspend fun addCollection(
        title: String,
        description: String?,
        userId: String
    ): UserCollectionModel

    suspend fun addCollection(
        title: String,
        description: String?,
        assignedNoteIds: Set<String>,
        userId: String
    ): UserCollectionModel

    // EDIT

    suspend fun updateCollectionTitle(collectionId: String, userId: String, newTitle: String): Boolean

    suspend fun updateCollectionDescription(collectionId: String, userId: String, newDescription: String?): Boolean

    suspend fun addNoteToCollection(collectionId: String, userId: String, noteId: String): Boolean

    suspend fun addNotesToCollection(collectionId: String, userId: String, noteIds: Set<String>): Boolean

    suspend fun removeNoteFromCollection(collectionId: String, userId: String, noteId: String): Boolean

    suspend fun removeNotesFromCollection(collectionId: String, userId: String, noteIds: Set<String>): Boolean

    // REMOVE

    suspend fun deleteCollectionById(collectionId: String, userId: String): Boolean

    suspend fun deleteCollectionByIds(collectionIds: Set<String>, userId: String): Boolean

    suspend fun deleteCollectionsForUser(userId: String): Boolean

}