package com.glitch.securenotes.data.model.entity

import kotlinx.serialization.Serializable

@Serializable
data class UserCredentialsModel(
    val userId: String,
    val login: String,
    val password: String
)
