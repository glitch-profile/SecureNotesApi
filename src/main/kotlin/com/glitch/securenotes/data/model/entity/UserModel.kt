package com.glitch.securenotes.data.model.entity

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import java.time.OffsetDateTime
import java.time.ZoneId

@Serializable
data class UserModel(
    @BsonId
    val id: String = BsonId().toString(),
    val username: String,
    val profileAvatar: FileModel,
    val syncedEncryptionKey: String?,
    val creationDate: Long = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond(),
    val activeSessions: List<String> = emptyList()
)
