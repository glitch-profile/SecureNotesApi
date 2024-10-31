package com.glitch.securenotes.data.cache.cachedmodel

import com.glitch.securenotes.data.model.entity.UserModel

data class CachedUserInfo(
    val userModel: UserModel,
    val lastUsesTimestamp: Long
)