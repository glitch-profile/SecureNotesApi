package com.glitch.securenotes.data.model.dto.collections

import kotlinx.serialization.Serializable

@Serializable
data class NewIncomingNoteCollectionDto(
    val title: String,
    val description: String? = null,
    val assignedNoteIds: Set<String> = emptySet()
)