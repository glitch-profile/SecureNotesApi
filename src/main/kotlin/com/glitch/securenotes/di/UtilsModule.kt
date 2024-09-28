package com.glitch.securenotes.di

import com.glitch.securenotes.data.datasource.AuthSessionStorage
import com.glitch.securenotes.data.datasourceimpl.AuthSessionStorageImpl
import com.glitch.securenotes.domain.utils.codeauth.CodeAuthenticator
import com.glitch.securenotes.domain.utils.codeauth.CodeAuthenticatorImpl
import org.koin.dsl.module

val utilsModule = module {
    single<AuthSessionStorage> {
        AuthSessionStorageImpl()
    }
    single<CodeAuthenticator> {
        CodeAuthenticatorImpl()
    }
}