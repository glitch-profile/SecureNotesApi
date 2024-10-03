package com.glitch.securenotes.data.model.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthIncomingCodeConfirmationDto(
    val code: String,
    val maxDurationHours: Int?
)
