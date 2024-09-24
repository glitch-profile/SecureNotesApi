package com.glitch.securenotes.data.model.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthIncomingLoginData(
    val login: String,
    val password: String
)
