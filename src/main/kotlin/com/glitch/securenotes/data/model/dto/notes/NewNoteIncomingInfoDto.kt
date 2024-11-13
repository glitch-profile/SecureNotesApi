package com.glitch.securenotes.data.model.dto.notes

import kotlinx.serialization.Serializable

@Serializable
data class NewNoteIncomingInfoDto(
    val title: String?,
    val description: String?,
    val text: String,
    val isSharing: Boolean = false,
    val editorUserIds: Set<String> = emptySet(),
    val readerUserIds: Set<String> = emptySet(),
    val createdTimestamp: Long? = null,
    val lastEditTimestamp: Long? = null
)