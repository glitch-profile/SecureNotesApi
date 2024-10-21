package com.glitch.securenotes.di

import com.glitch.securenotes.data.datasource.UserCredentialsDataSource
import com.glitch.securenotes.data.datasource.UsersDataSource
import com.glitch.securenotes.data.datasource.notes.NotesDataSource
import com.glitch.securenotes.data.datasourceimpl.NotesDataSourceImpl
import com.glitch.securenotes.data.datasourceimpl.UserCredentialsDataSourceImpl
import com.glitch.securenotes.data.datasourceimpl.UsersDataSourceImpl
import com.glitch.securenotes.domain.utils.notescache.NoteInfoCacheManager
import com.glitch.securenotes.domain.utils.notescache.NoteInfoCacheManagerImpl
import org.koin.dsl.module

val dataSourceModule = module {
    single<UserCredentialsDataSource> {
        UserCredentialsDataSourceImpl(get())
    }
    single<UsersDataSource> {
        UsersDataSourceImpl(get())
    }
    single<NotesDataSource> {
        NotesDataSourceImpl(get())
    }
    single<NoteInfoCacheManager> {
        NoteInfoCacheManagerImpl()
    }
}