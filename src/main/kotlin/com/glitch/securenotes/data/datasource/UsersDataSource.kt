package com.glitch.securenotes.data.datasource

import com.glitch.securenotes.data.model.entity.FileModel
import com.glitch.securenotes.data.model.entity.UserModel

interface UsersDataSource {

    suspend fun getUserById(userId: String): UserModel

    suspend fun getUsersById(userIds: List<String>): List<UserModel>

    suspend fun addUser(
        userName: String,
        profileAvatar: FileModel? = null,
        syncedEncryptionKey: String? = null
    ): UserModel

    suspend fun deleteUserById(userId: String): Boolean

    suspend fun updateUserById(userId: String, newUserModel: UserModel): Boolean

    suspend fun getUserEncryptionKey(userId: String): String?

    suspend fun enableEncryptionKeySync(userId: String, encryptionKey: String): Boolean

    suspend fun disableEncryptionKeySync(userId: String): Boolean

    suspend fun updateUsername(userId: String, newUsername: String): Boolean

    suspend fun updateUserProfileAvatar(userId: String, imageInfo: FileModel?): Boolean

    suspend fun addActiveSessionId(userId: String, sessionId: String): Boolean

    suspend fun removeActiveSessionId(userId: String, sessionId: String): Boolean

    suspend fun removeActiveSessionId(userId: String, sessionsIds: List<String>): Boolean

}