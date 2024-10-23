package com.glitch.securenotes.data.model.entity

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import java.time.OffsetDateTime
import java.time.ZoneId

@Serializable
data class FileModel(
    val fileId: String = BsonId().toString(),
    val name: String,
    val urlPath: String,
    val previewUrlPath: String? = null,
    val creationTimestamp: Long = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
)
