package com.glitch.securenotes.data.model.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthSessionOutgoingDto(
    val id: String,
    val platform: String,
    val agentName: String,
    val lastActiveTimestamp: Long
)
