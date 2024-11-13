package com.glitch.securenotes.data.datasourceimpl

import com.glitch.floweryapi.domain.utils.encryptor.AESEncryptor
import com.glitch.securenotes.data.datasource.UserCredentialsDataSource
import com.glitch.securenotes.data.exceptions.auth.CredentialsNotFoundException
import com.glitch.securenotes.data.exceptions.auth.IncorrectCredentialsException
import com.glitch.securenotes.data.exceptions.auth.LoginAlreadyInUseException
import com.glitch.securenotes.data.model.entity.UserCredentialsModel
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.singleOrNull

class UserCredentialsDataSourceImpl(
    database: MongoDatabase
): UserCredentialsDataSource {

    private val credentials = database.getCollection<UserCredentialsModel>("AuthCredentials")

    override suspend fun auth(login: String, password: String): String {
        val loginEncrypted = AESEncryptor.encrypt(login)
        val passwordEncrypted = AESEncryptor.encrypt(password)
        val filter = Filters.and(
            Filters.eq(UserCredentialsModel::login.name, loginEncrypted),
            Filters.eq(UserCredentialsModel::password.name, passwordEncrypted)
        )
        val result = credentials.find(filter).singleOrNull() ?: throw CredentialsNotFoundException()
        return result.userId
    }

    override suspend fun getCredentials(userId: String): UserCredentialsModel {
        val filter = Filters.eq(UserCredentialsModel::userId.name, userId)
        val result = credentials.find(filter).singleOrNull() ?: throw CredentialsNotFoundException()
        return UserCredentialsModel(
            userId = result.userId,
            login = AESEncryptor.decrypt(result.login),
            password = AESEncryptor.decrypt(result.password)
        )
    }

    override suspend fun addCredentials(userId: String, login: String, password: String): UserCredentialsModel {
        val loginEncrypted = AESEncryptor.encrypt(login)
        val equalityFilter = Filters.eq(UserCredentialsModel::login.name, loginEncrypted)
        val otherCredentials = credentials.find(equalityFilter).singleOrNull()
        if (otherCredentials != null) throw LoginAlreadyInUseException()
        val passwordEncrypted = AESEncryptor.encrypt(password)
        val newCredentials = UserCredentialsModel(
            userId = userId,
            login = loginEncrypted,
            password = passwordEncrypted
        )
        val result = credentials.insertOne(newCredentials)
        if (result.insertedId != null) return newCredentials
        else throw IncorrectCredentialsException()
    }

    override suspend fun updateCredentials(
        userId: String,
        oldPassword: String,
        newPassword: String
    ): Boolean {
        val filter = Filters.eq(UserCredentialsModel::userId.name, userId)
        val credentialData = credentials.find(filter).singleOrNull() ?: throw CredentialsNotFoundException()
        val currentPasswordDecrypted = AESEncryptor.decrypt(credentialData.password)
        if (currentPasswordDecrypted != oldPassword) throw IncorrectCredentialsException()
        val newPasswordEncrypted = AESEncryptor.encrypt(newPassword)
        val update = Updates.set(UserCredentialsModel::password.name, newPasswordEncrypted)
        val result = credentials.updateOne(filter, update)
        if (result.modifiedCount != 0L)
            return true
        else throw IncorrectCredentialsException()
    }

    override suspend fun deleteCredentials(userId: String): Boolean {
        val filter = Filters.eq(UserCredentialsModel::userId.name, userId)
        val result = credentials.deleteOne(filter)
        return result.deletedCount != 0L
    }
}