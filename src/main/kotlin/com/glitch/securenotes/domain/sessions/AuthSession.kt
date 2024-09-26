package com.glitch.securenotes.domain.sessions

import kotlinx.serialization.Serializable

@Serializable
data class AuthSession(
    val userId: String?,
    val expireTimestamp: Long? // null is never expire
)
