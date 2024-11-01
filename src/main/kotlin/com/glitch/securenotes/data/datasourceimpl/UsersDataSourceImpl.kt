package com.glitch.securenotes.data.datasourceimpl

import com.glitch.floweryapi.domain.utils.encryptor.AESEncryptor
import com.glitch.securenotes.data.cache.datacache.UsersDataCache
import com.glitch.securenotes.data.datasource.UsersDataSource
import com.glitch.securenotes.data.exceptions.users.IncorrectSecuredNotesPasswordException
import com.glitch.securenotes.data.exceptions.users.ProtectedNotesNotConfiguredException
import com.glitch.securenotes.data.exceptions.users.UserNotFoundException
import com.glitch.securenotes.data.model.entity.FileModel
import com.glitch.securenotes.data.model.entity.UserModel
import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList

// TODO: Add encryption with server's secret
class UsersDataSourceImpl(
    db: MongoDatabase,
    private val usersCache: UsersDataCache
) : UsersDataSource {

    private val users = db.getCollection<UserModel>("Users")

    override suspend fun getOneUserById(userId: String): UserModel {
        if (usersCache.isUserIdExists(userId)) {
            return usersCache.getUserById(userId)!!
        } else {
            val filter = Filters.eq("_id", userId)
            val user =  users.find(filter).singleOrNull() ?: throw UserNotFoundException()
            usersCache.addUserToCache(user)
            return user
        }
    }

    override suspend fun getManyUsersById(userIds: List<String>): List<UserModel> {
        val filter = Filters.`in`("_id", userIds)
        return users.find(filter).toList()
    }

    override suspend fun addUser(
        userName: String,
        profileAvatar: FileModel?,
    ): UserModel {
        val userModel = UserModel(
            username = userName,
            profileAvatar = profileAvatar
        )
        users.insertOne(userModel)
        usersCache.addUserToCache(userModel)
        return userModel
    }

    override suspend fun deleteUserById(userId: String): Boolean {
        val filter = Filters.eq("_id", userId)
        val result = users.deleteOne(filter).deletedCount != 0L
        if (result) usersCache.deleteUserById(userId)
        return result
    }

    @Deprecated("use update*param* functions instead", level = DeprecationLevel.ERROR)
    override suspend fun updateUserById(userId: String, newUserModel: UserModel): Boolean {
        val filter = Filters.eq("_id", userId)
        val result = users.replaceOne(filter, newUserModel)
        if (result.matchedCount != 0L) return result.modifiedCount != 0L
        else throw UserNotFoundException()
    }

    override suspend fun updateUsername(userId: String, newUsername: String): Boolean {
        val filter = Filters.eq("_id", userId)
        val update = Updates.set(UserModel::username.name, newUsername)
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val updatedUser = users.findOneAndUpdate(filter, update, updateOptions)
        if (updatedUser !== null) {
            usersCache.updateSavedUser(updatedUser)
            return true
        } else throw UserNotFoundException()
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
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val updatedUser = users.findOneAndUpdate(filter, update, updateOptions)
        if (updatedUser != null) {
            usersCache.updateSavedUser(updatedUser)
            return true
        } else throw UserNotFoundException()
    }

    override suspend fun clearUserProfileAvatar(userId: String): Boolean {
        val filter = Filters.eq("_id", userId)
        val update = Updates.set(UserModel::profileAvatar.name, null)
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val updatedUser = users.findOneAndUpdate(filter, update, updateOptions)
        if (updatedUser != null) {
            usersCache.updateSavedUser(updatedUser)
            return true
        } else throw UserNotFoundException()
    }

    override suspend fun updateUserProtectedNotesPassword(
        userId: String,
        oldPassword: String?,
        newPassword: String
    ): Boolean {
        val user = getOneUserById(userId)
        val oldPasswordEncrypted = if (oldPassword != null) {
            AESEncryptor.encrypt(oldPassword)
        } else null
        if (user.protectedNotePassword == oldPasswordEncrypted) {
            val newPasswordEncrypted = AESEncryptor.encrypt(newPassword)
            val filter = Filters.eq("_id", userId)
            val update = Updates.set(UserModel::protectedNotePassword.name, newPasswordEncrypted)
            val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
            val updatedUser = users.findOneAndUpdate(filter, update, updateOptions)
            if (updatedUser != null) {
                usersCache.updateSavedUser(updatedUser)
                return true
            } else throw UserNotFoundException()
        } else throw IncorrectSecuredNotesPasswordException()
    }

    override suspend fun resetUserProtectedNotesPassword(userId: String): Boolean {
        val filter = Filters.eq("_id", userId)
        val update = Updates.combine(
            Updates.unset(UserModel::protectedNotePassword.name),
            Updates.set(UserModel::protectedNoteIds.name, emptyList<String>())
        )
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val updatedUser = users.findOneAndUpdate(filter, update, updateOptions)
        if (updatedUser != null) {
            usersCache.updateSavedUser(updatedUser)
            return true
        } else throw UserNotFoundException()
    }

    override suspend fun addNoteToProtected(userId: String, noteId: String): Boolean {
        val user = getOneUserById(userId)
        if (user.protectedNotePassword != null) {
            val filter = Filters.eq("_id", userId)
            val update = Updates.addToSet(UserModel::protectedNoteIds.name, noteId)
            val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
            val updatedUser = users.findOneAndUpdate(filter, update, updateOptions)
            if (updatedUser != null) {
                usersCache.updateSavedUser(updatedUser)
                return true
            } else throw UserNotFoundException()
        } else throw ProtectedNotesNotConfiguredException()

    }

    override suspend fun removeNoteFromProtected(userId: String, noteId: String, protectedNotesPassword: String): Boolean {
        val user = getOneUserById(userId)
        if (user.protectedNotePassword != null) {
            val passwordEncrypted = AESEncryptor.encrypt(protectedNotesPassword)
            if (passwordEncrypted == user.protectedNotePassword) {
                val filter = Filters.eq("_id", userId)
                val update = Updates.pull(UserModel::protectedNoteIds.name, noteId)
                val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
                val updatedUser = users.findOneAndUpdate(filter, update, updateOptions)
                if (updatedUser != null) {
                    usersCache.updateSavedUser(updatedUser)
                    return true
                } else throw UserNotFoundException()
            } else throw IncorrectSecuredNotesPasswordException()
        } else throw ProtectedNotesNotConfiguredException()
    }

    override suspend fun addActiveSessionId(userId: String, sessionId: String): Boolean {
        val filter = Filters.eq("_id", userId)
        val update = Updates.addToSet(UserModel::activeSessions.name, sessionId)
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val updatedUser = users.findOneAndUpdate(filter, update, updateOptions)
        if (updatedUser != null) {
            usersCache.updateSavedUser(updatedUser)
            return true
        } else throw UserNotFoundException()
    }

    override suspend fun removeActiveSessionId(userId: String, sessionId: String): Boolean {
        val filter = Filters.eq("_id", userId)
        val update = Updates.pull(UserModel::activeSessions.name, sessionId)
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val updatedUser = users.findOneAndUpdate(filter, update, updateOptions)
        if (updatedUser != null) {
            usersCache.updateSavedUser(updatedUser)
            return true
        } else throw UserNotFoundException()
    }

    override suspend fun removeActiveSessionId(userId: String, sessionsIds: List<String>): Boolean {
        val filter = Filters.eq("_id", userId)
        val update = Updates.pullAll(UserModel::activeSessions.name, sessionsIds)
        val updateOptions = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        val updatedUser = users.findOneAndUpdate(filter, update, updateOptions)
        if (updatedUser != null) {
            usersCache.updateSavedUser(updatedUser)
            return true
        } else throw UserNotFoundException()
    }
}