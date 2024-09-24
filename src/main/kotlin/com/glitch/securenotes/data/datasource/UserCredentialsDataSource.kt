package com.glitch.securenotes.data.datasource

import com.glitch.securenotes.data.model.entity.UserCredentialsModel

interface UserCredentialsDataSource {

    suspend fun auth(
        login: String,
        password: String
    ): String

    suspend fun getCredentials(userId: String): UserCredentialsModel

    suspend fun addCredentials(
        userId: String,
        login: String,
        password: String
    ): UserCredentialsModel

    suspend fun updateCredentials(
        userId: String,
        oldPassword: String,
        newPassword: String
    ): Boolean

    suspend fun deleteCredentials(userId: String): Boolean

}