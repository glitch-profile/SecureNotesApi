package com.glitch.securenotes.data.model.dto.notes

import kotlinx.serialization.Serializable

@Serializable
data class NoteSharingStatusDto(
    val isSharing: Boolean,
    val creatorId: String,
    val editorIds: Set<String>,
    val readerIds: Set<String>
)
