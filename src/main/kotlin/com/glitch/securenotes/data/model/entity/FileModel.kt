package com.glitch.securenotes.data.model.entity

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId

@Serializable
data class FileModel(
    val fileId: String = BsonId().toString(),
    val name: String,
    val description: String,
    val extension: String,
    val urlPath: String,
    val previewUrlPath: String?
)
