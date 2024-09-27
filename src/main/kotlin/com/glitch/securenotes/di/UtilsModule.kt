package com.glitch.securenotes.di

import com.glitch.securenotes.data.datasource.AuthSessionStorage
import com.glitch.securenotes.data.datasourceimpl.AuthSessionStorageImpl
import com.glitch.securenotes.domain.utils.codeauth.CodeAuthenticator
import com.glitch.securenotes.domain.utils.codeauth.CodeAuthenticatorImpl
import io.ktor.server.config.*
import io.ktor.server.sessions.*
import org.koin.dsl.module
import java.io.File
import java.nio.file.Paths

private val isPackedForExternal = ApplicationConfig(null).tryGetString("app.is_for_external").toBoolean()
private val sessionsStorageDir = if (isPackedForExternal) File("${Paths.get("")}/sessions")
    else File("build/.sessions")

val utilsModule = module {
    single<AuthSessionStorage> {
        AuthSessionStorageImpl()
    }
    single<CodeAuthenticator> {
        CodeAuthenticatorImpl()
    }
}