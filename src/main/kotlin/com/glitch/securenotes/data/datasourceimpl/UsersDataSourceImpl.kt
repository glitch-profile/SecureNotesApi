package com.glitch.securenotes.data.datasourceimpl

import com.glitch.floweryapi.domain.utils.encryptor.AESEncryptor
import com.glitch.securenotes.data.datasource.UsersDataSource
import com.glitch.securenotes.data.exceptions.users.UserNotFoundException
import com.glitch.securenotes.data.model.entity.FileModel
import com.glitch.securenotes.data.model.entity.UserModel
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList

class UsersDataSourceImpl(
    database: MongoDatabase
) : UsersDataSource {

    private val users = database.getCollection<UserModel>("Users")

    override suspend fun getUserById(userId: String): UserModel {
        val filter = Filters.eq("_id", userId)
        return users.find(filter).singleOrNull() ?: throw UserNotFoundException()
    }

    override suspend fun getUsersById(userIds: List<String>): List<UserModel> {
        val filter = Filters.`in`("_id", userIds)
        return users.find(filter).toList()
    }

    override suspend fun addUser(
        userName: String,
        profileAvatar: FileModel?,
        syncedEncryptionKey: String?
    ): UserModel {
        val userModel = UserModel(
            username = userName,
            profileAvatar = profileAvatar,
            syncedEncryptionKey = syncedEncryptionKey
        )
        val result = users.insertOne(userModel)
        return userModel
    }

    override suspend fun deleteUserById(userId: String): Boolean {
        val filter = Filters.eq("_id", userId)
        val result = users.deleteOne(filter)
        if (result.deletedCount != 0L) return true
        else throw UserNotFoundException()
    }

    override suspend fun updateUserById(userId: String, newUserModel: UserModel): Boolean {
        val filter = Filters.eq("_id", userId)
        val result = users.replaceOne(filter, newUserModel)
        if (result.matchedCount != 0L) return true
        else throw UserNotFoundException()
    }

    override suspend fun getUserEncryptionKey(userId: String): String? {
        val filter = Filters.eq("_id", userId)
        val userModel = users.find(filter).singleOrNull() ?: throw UserNotFoundException()
        return if (userModel.syncedEncryptionKey != null) {
            AESEncryptor.decrypt(userModel.syncedEncryptionKey)
        } else null
    }

    override suspend fun enableEncryptionKeySync(userId: String, encryptionKey: String): Boolean {
        val filter = Filters.eq("_id", userId)
        val protectedEncryptionKey = AESEncryptor.encrypt(encryptionKey)
        val update = Updates.set(UserModel::syncedEncryptionKey.name, protectedEncryptionKey)
        val result = users.updateOne(filter, update)
        if (result.matchedCount != 0L) return true
        else throw UserNotFoundException()
    }

    override suspend fun disableEncryptionKeySync(userId: String): Boolean {
        val filter = Filters.eq("_id", userId)
        val update = Updates.set(UserModel::syncedEncryptionKey.name, null)
        val result = users.updateOne(filter, update)
        if (result.matchedCount != 0L) return true
        else throw UserNotFoundException()
    }

    override suspend fun updateUsername(userId: String, newUsername: String): Boolean {
        val filter = Filters.eq("_id", userId)
        val update = Updates.set(UserModel::username.name, newUsername)
        val result = users.updateOne(filter, update)
        if (result.matchedCount != 0L) return true
        else throw UserNotFoundException()
    }

    override suspend fun updateUserProfileAvatar(
        userId: String,
        avatarUrlPath: String,
        avatarThumbnailUrlPath: String
    ): Boolean {
        val filter = Filters.eq("_id", userId)
        val imageInfo = FileModel(
            name = "avatar.jpg",
            urlPath = avatarUrlPath,
            previewUrlPath = avatarThumbnailUrlPath
        )
        val update = Updates.set(UserModel::profileAvatar.name, imageInfo)
        val result = users.updateOne(filter, update)
        if (result.matchedCount != 0L) return true
        else throw UserNotFoundException()
    }

    override suspend fun clearUserProfileAvatar(userId: String): Boolean {
        val filter = Filters.eq("_id", userId)
        val update = Updates.set(UserModel::profileAvatar.name, null)
        val result = users.updateOne(filter, update)
        if (result.matchedCount != 0L) return true
        else throw UserNotFoundException()
    }

    override suspend fun addActiveSessionId(userId: String, sessionId: String): Boolean {
        val filter = Filters.eq("_id", userId)
        val update = Updates.addToSet(UserModel::activeSessions.name, sessionId)
        val result = users.updateOne(filter, update)
        if (result.matchedCount != 0L) return true
        else throw UserNotFoundException()
    }

    override suspend fun removeActiveSessionId(userId: String, sessionId: String): Boolean {
        val filter = Filters.eq("_id", userId)
        val update = Updates.pull(UserModel::activeSessions.name, sessionId)
        val result = users.updateOne(filter, update)
        if (result.matchedCount != 0L) return true
        else throw UserNotFoundException()
    }

    override suspend fun removeActiveSessionId(userId: String, sessionsIds: List<String>): Boolean {
        val filter = Filters.eq("_id", userId)
        val update = Updates.pullAll(UserModel::activeSessions.name, sessionsIds)
        val result = users.updateOne(filter, update)
        if (result.matchedCount != 0L) return true
        else throw UserNotFoundException()
    }
}