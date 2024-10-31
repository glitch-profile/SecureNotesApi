package com.glitch.securenotes.data.cache.datacache

import com.glitch.securenotes.data.model.entity.UserModel

interface UsersDataCache {

    fun getUserById(userId: String): UserModel?

    fun isUserIdExists(userId: String): Boolean

    fun addUserToCache(user: UserModel)

    fun updateSavedUser(userModel: UserModel)

    fun deleteUserById(userId: String)

}