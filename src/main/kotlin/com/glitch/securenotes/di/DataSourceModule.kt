package com.glitch.securenotes.di

import com.glitch.securenotes.data.datasource.UserCredentialsDataSource
import com.glitch.securenotes.data.datasource.UsersDataSource
import com.glitch.securenotes.data.datasourceimpl.UserCredentialsDataSourceImpl
import com.glitch.securenotes.data.datasourceimpl.UsersDataSourceImpl
import org.koin.dsl.module

val dataSourceModule = module {
    single<UserCredentialsDataSource> {
        UserCredentialsDataSourceImpl(get())
    }
    single<UsersDataSource> {
        UsersDataSourceImpl(get())
    }
}