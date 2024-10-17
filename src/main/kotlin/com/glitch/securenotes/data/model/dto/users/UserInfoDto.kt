package com.glitch.securenotes.data.model.dto.users

import com.glitch.securenotes.data.model.entity.FileModel

data class UserInfoDto(
    val id: String,
    val username: String,
    val profileImage: FileModel?,
    val accountCreationTimestamp: Long? = null
)
