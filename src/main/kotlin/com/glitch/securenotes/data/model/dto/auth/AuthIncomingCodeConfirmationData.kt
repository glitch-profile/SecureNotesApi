package com.glitch.securenotes.data.model.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthIncomingCodeConfirmationData(
    val code: String,
    val maxDurationHours: Int?
)
