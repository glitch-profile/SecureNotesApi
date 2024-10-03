package com.glitch.securenotes.data.model.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthOutgoingInfo(
    val sessionId: String,
    val userId: String
)