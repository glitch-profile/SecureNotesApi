package com.glitch.securenotes.data.model.dto.notes

import kotlinx.serialization.Serializable

@Serializable
data class NoteCompactInfoDto(
    val id: String,
    val title: String? = null,
    val description: String? = null,
    val text: String,
    val isSharing: Boolean,
    val userRole: Short,
    val creationTimestamp: Long,
    val lastEditTimestamp: Long?
)
