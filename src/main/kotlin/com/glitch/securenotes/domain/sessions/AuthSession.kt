package com.glitch.securenotes.domain.sessions

import kotlinx.serialization.Serializable

@Serializable
data class AuthSession(
    val userId: String,
    val platformName: String,
    val appVersionString: String,
    val lastActivityTimestamp: Long,
    val durationInHours: Int? // null is never expire
)
