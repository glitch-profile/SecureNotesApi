package com.glitch.securenotes.data.model.dto.utils

import kotlinx.serialization.Serializable

@Serializable
data class PingInfoDto(
    val currentServerMillis: Long,
    val hostAddress: String,
    val remoteAddress: String
)
