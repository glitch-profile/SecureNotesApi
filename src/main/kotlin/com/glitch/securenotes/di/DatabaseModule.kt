package com.glitch.securenotes.di

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.ktor.server.config.*
import org.koin.dsl.module

private val mongoUri = ApplicationConfig(null).tryGetString("app.database.mongo_uri")!!
private const val DATABASE_NAME = "SecureNotes"

val databaseModule = module {
    single<MongoDatabase> {
        MongoClient
            .create(mongoUri)
            .getDatabase(DATABASE_NAME)
    }
}