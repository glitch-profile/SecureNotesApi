package com.glitch.securenotes.di

import com.glitch.securenotes.data.datasource.AuthSessionStorage
import com.glitch.securenotes.data.datasourceimpl.AuthSessionStorageImpl
import com.glitch.securenotes.domain.utils.codeauth.CodeAuthenticator
import com.glitch.securenotes.domain.utils.codeauth.CodeAuthenticatorImpl
import com.glitch.securenotes.domain.utils.filemanager.FileManager
import com.glitch.securenotes.domain.utils.filemanager.FileManagerImpl
import com.glitch.securenotes.domain.utils.imageprocessor.ImageProcessor
import com.glitch.securenotes.domain.utils.imageprocessor.ImageProcessorImpl
import org.koin.dsl.module

val utilsModule = module {
    single<AuthSessionStorage> {
        AuthSessionStorageImpl()
    }
    single<CodeAuthenticator> {
        CodeAuthenticatorImpl()
    }
    single<FileManager> {
        FileManagerImpl()
    }
    single<ImageProcessor> {
        ImageProcessorImpl()
    }

}