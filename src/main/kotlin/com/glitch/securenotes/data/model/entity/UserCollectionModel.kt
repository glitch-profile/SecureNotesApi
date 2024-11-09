package com.glitch.securenotes.data.model.entity

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId

// TODO: Add collections implementation
@Serializable
data class UserCollectionModel(
    @BsonId
    val id: String = BsonId().toString(),
    val title: String,
    val description: String? = null,
    val userId: String,
    val assignedNotes: List<String> = emptyList()
)
