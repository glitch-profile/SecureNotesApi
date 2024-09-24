package com.glitch.securenotes.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserCredentialsInfo(
    val userId: String,
    val login: String,
    val password: String
)
