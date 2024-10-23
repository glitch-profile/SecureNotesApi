package com.glitch.securenotes.data.model.entity

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import java.time.OffsetDateTime
import java.time.ZoneId

@Serializable
data class ResourceModel(
    val id: String = BsonId().toString(),
    val noteId: String,
    val file: FileModel,
    val title: String,
    val description: String? = null,
    val createdTimestamp: Long = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond(),
    val createdUserId: String,
    val reusedUserId: String? = null
)