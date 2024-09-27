package com.glitch.securenotes.data.model.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthSocketEventData(
    val eventCode: Int,
    val data: String?
)
