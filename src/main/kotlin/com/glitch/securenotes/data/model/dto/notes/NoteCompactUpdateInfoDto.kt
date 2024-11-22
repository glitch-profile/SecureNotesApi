package com.glitch.securenotes.data.model.dto.notes

import kotlinx.serialization.Serializable

@Serializable
data class NoteCompactUpdateInfoDto(
    val id: String,
    val title: String?,
    val description: String?,
    val text: String,
    val lastEditTimestamp: Long?
)
