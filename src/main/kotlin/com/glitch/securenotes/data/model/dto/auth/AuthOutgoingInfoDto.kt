package com.glitch.securenotes.data.model.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthOutgoingInfoDto(
    val sessionId: String,
    val userId: String
)