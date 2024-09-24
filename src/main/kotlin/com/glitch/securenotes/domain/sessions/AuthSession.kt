package com.glitch.securenotes.domain.sessions

import kotlinx.serialization.Serializable

@Serializable
data class AuthSession(
    val isSessionConfirmed: Boolean,
    val userId: String?,
    val expireTimestamp: Long?
)
