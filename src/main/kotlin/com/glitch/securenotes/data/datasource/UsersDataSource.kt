package com.glitch.securenotes.data.datasource

import com.glitch.securenotes.data.model.entity.FileModel
import com.glitch.securenotes.data.model.entity.UserModel

interface UsersDataSource {

    //GENERAL

    suspend fun getOneUserById(userId: String): UserModel

    suspend fun getManyUsersById(userIds: List<String>): List<UserModel>

    suspend fun addUser(
        userName: String,
        profileAvatar: FileModel? = null,
        syncedEncryptionKey: String? = null
    ): UserModel

    suspend fun deleteUserById(userId: String): Boolean

    suspend fun updateUserById(userId: String, newUserModel: UserModel): Boolean

    // ENCRYPTION KEY

    suspend fun getUserEncryptionKey(userId: String): String?

    suspend fun enableEncryptionKeySync(userId: String, encryptionKey: String): Boolean

    suspend fun disableEncryptionKeySync(userId: String): Boolean

    // GENERAL USER INFO

    suspend fun updateUsername(userId: String, newUsername: String): Boolean

    // USER AVATAR

    suspend fun updateUserProfileAvatar(
        userId: String,
        avatarUrlPath: String,
        avatarThumbnailUrlPath: String
    ): Boolean

    suspend fun clearUserProfileAvatar(userId: String): Boolean

    // PROTECTED NOTES

    suspend fun updateUserProtectedNotesPassword(
        userId: String,
        oldPassword: String?,
        newPassword: String
    ): Boolean

    suspend fun resetUserProtectedNotesPassword(
        userId: String
    ): Boolean

    suspend fun addNoteToProtected(userId: String, noteId: String): Boolean

    suspend fun removeNoteFromProtected(
        userId: String,
        noteId: String,
        protectedNotesPassword: String
    ): Boolean

    // SESSIONS

    suspend fun addActiveSessionId(userId: String, sessionId: String): Boolean

    suspend fun removeActiveSessionId(userId: String, sessionId: String): Boolean

    suspend fun removeActiveSessionId(userId: String, sessionsIds: List<String>): Boolean

}