package com.glitch.securenotes.data.model.entity

import com.glitch.securenotes.data.exceptions.notes.NoteNotFoundException
import com.glitch.securenotes.data.model.dto.notes.NoteCompactInfoDto
import com.glitch.securenotes.data.model.dto.notes.NoteCompactUpdateInfoDto
import com.glitch.securenotes.domain.utils.UserRoleCode
import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.time.OffsetDateTime
import java.time.ZoneId

@Serializable
data class NoteModel(
    @BsonId
    val id: String = ObjectId().toString(),
    val creatorId: String,
    val encryptionKey: String,
    val isSharing: Boolean = false,
    val sharedEditorUserIds: Set<String> = emptySet(),
    val sharedReaderUserIds: Set<String> = emptySet(),
    val creationTimestamp: Long = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond(),
    val lastEditTimestamp: Long? = null,
    val title: String?,
    val description: String?,
    val text: String
) {

    fun getAllUsers(): List<String> = if (isSharing) {
        mutableListOf<String>().apply {
            add(creatorId)
            addAll(sharedEditorUserIds)
            addAll(sharedReaderUserIds)
        }.toList()
    } else listOf(creatorId)

    fun getSharedUsers(): List<String> = if (isSharing) {
        mutableListOf<String>().apply {
            addAll(sharedEditorUserIds)
            addAll(sharedReaderUserIds)
        }.toList()
    } else emptyList()

    fun toCompactInfo(requestedUserId: String): NoteCompactInfoDto {
        val userRole = when (requestedUserId) {
            creatorId -> UserRoleCode.ROLE_OWNER
            in sharedEditorUserIds -> UserRoleCode.ROLE_EDITOR
            in sharedReaderUserIds -> UserRoleCode.ROLE_READER
            else -> throw NoteNotFoundException()
        }
        return NoteCompactInfoDto(
            id = id,
            title = title,
            description = description,
            text = text,
            isSharing = isSharing,
            userRole = userRole,
            creationTimestamp = creationTimestamp,
            lastEditTimestamp = lastEditTimestamp
        )
    }

    fun toCompactInfo(userRoleCode: Short): NoteCompactInfoDto {
        return NoteCompactInfoDto(
            id = id,
            title = title,
            description = description,
            text = text,
            isSharing = isSharing,
            userRole = userRoleCode,
            creationTimestamp = creationTimestamp,
            lastEditTimestamp = lastEditTimestamp
        )
    }

    fun toCompactRoomSocketInfo(): NoteCompactUpdateInfoDto {
        return NoteCompactUpdateInfoDto(
            id = id,
            title = title,
            description = description,
            text = text,
            lastEditTimestamp = lastEditTimestamp
        )
    }

}
