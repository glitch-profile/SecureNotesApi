package com.glitch.securenotes.data.model.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthIncomingNewAccountDto(
    val username: String,
    val login: String,
    val password: String
)
