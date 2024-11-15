package com.glitch.securenotes.data.model.dto.notes

import kotlinx.serialization.Serializable

@Serializable
data class UserListsIncomingDto(
    val editors: Set<String> = emptySet(),
    val readers: Set<String> = emptySet()
)