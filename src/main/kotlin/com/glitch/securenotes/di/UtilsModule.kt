package com.glitch.securenotes.di

import io.ktor.server.config.*
import io.ktor.server.sessions.*
import org.koin.dsl.module
import java.io.File
import java.nio.file.Paths

private val isPackedForExternal = ApplicationConfig(null).tryGetString("app.is_for_external").toBoolean()
private val sessionsStorageDir = if (isPackedForExternal) File("${Paths.get("")}/sessions")
    else File("build/.sessions")

val utilsModule = module {
    single<SessionStorage> {
        directorySessionStorage(sessionsStorageDir)
    }
}