package com.glitch.securenotes.data.model.entity

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.time.OffsetDateTime
import java.time.ZoneId

@Serializable
data class ResourceModel(
    @BsonId
    val id: String = ObjectId().toString(),
    val noteId: String,
    val file: FileModel,
    val title: String,
    val description: String? = null,
    val createdTimestamp: Long = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond(),
    val lastEditTimestamp: Long? = null,
    val createdUserId: String
)