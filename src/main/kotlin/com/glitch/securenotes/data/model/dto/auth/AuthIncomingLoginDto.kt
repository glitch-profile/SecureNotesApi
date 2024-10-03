package com.glitch.securenotes.data.model.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthIncomingLoginDto(
    val login: String,
    val password: String
)
