package com.glitch.securenotes.data.model.entity

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

// TODO: Add collections implementation
@Serializable
data class UserCollectionModel(
    @BsonId
    val id: String = ObjectId().toString(),
    val title: String,
    val description: String? = null,
    val userId: String,
    val assignedNotes: List<String> = emptyList()
)
