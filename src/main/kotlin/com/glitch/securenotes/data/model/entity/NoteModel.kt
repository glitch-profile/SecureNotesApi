package com.glitch.securenotes.data.model.entity

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import java.time.OffsetDateTime
import java.time.ZoneId

@Serializable
data class NoteModel(
    @BsonId
    val id: String = BsonId().toString(),
    val creatorId: String,
    val sharedUsersIds: List<String> = emptyList(),
    val sharedEncryptionKey: String?,
    val creationTimestamp: Long = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond(),
    val lastEditDate: Long? = null,
    val title: String?,
    val description: String?,
    val text: String,
    val noteResources: List<FileModel>
)
