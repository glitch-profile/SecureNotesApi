package com.glitch.securenotes.data.model.dto.users

import kotlinx.serialization.Serializable

@Serializable
data class UserUpdatePasswordDto(
    val oldPassword: String?,
    val newPassword: String?
)
