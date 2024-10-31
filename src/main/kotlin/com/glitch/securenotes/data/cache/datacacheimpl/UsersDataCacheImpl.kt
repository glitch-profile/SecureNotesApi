package com.glitch.securenotes.data.cache.datacacheimpl

import com.glitch.securenotes.data.cache.cachedmodel.CachedUserInfo
import com.glitch.securenotes.data.cache.datacache.UsersDataCache
import com.glitch.securenotes.data.model.entity.UserModel
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

class UsersDataCacheImpl(
    private val maxCacheSize: Int
): UsersDataCache {

    private val users = ConcurrentHashMap<String, CachedUserInfo>()

    override fun getUserById(userId: String): UserModel? {
        if (users.containsKey(userId)) {
            val info = users[userId]!!
            users[userId] = info.copy(
                lastUsesTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            )
            return info.userModel
        }
        else return null
    }

    override fun isUserIdExists(userId: String): Boolean {
        return users.containsKey(userId)
    }

    override fun addUserToCache(user: UserModel) {
        if (!isUserIdExists(user.id)) {
            if (users.size >= maxCacheSize) {
                val oldestElement = users.minByOrNull { it.value.lastUsesTimestamp }!!
                users.remove(oldestElement.key)
            }
            users[user.id] = CachedUserInfo(
                userModel = user,
                lastUsesTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            )
        }
    }

    override fun updateSavedUser(userModel: UserModel) {
        if (isUserIdExists(userModel.id)) {
            users[userModel.id] = CachedUserInfo(
                userModel = userModel,
                lastUsesTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            )
        } else addUserToCache(userModel)
    }

    override fun deleteUserById(userId: String) {
        users.remove(userId)
    }
}