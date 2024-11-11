package com.glitch.securenotes.data.model.entity

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.time.OffsetDateTime
import java.time.ZoneId

@Serializable
data class UserModel(
    @BsonId
    val id: String = ObjectId().toString(),
    val username: String,
    val profileAvatar: FileModel?,
    val protectedNotePassword: String? = null,
    val protectedNoteIds: Set<String> = emptySet(),
    val creationDate: Long = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond(),
    val activeSessions: Set<String> = emptySet()
)
