package com.glitch.securenotes.di

import com.glitch.securenotes.data.cache.datacache.NoteResourcesDataCache
import com.glitch.securenotes.data.cache.datacache.NotesDataCache
import com.glitch.securenotes.data.cache.datacache.UserCollectionsDataCache
import com.glitch.securenotes.data.cache.datacache.UsersDataCache
import com.glitch.securenotes.data.cache.datacacheimpl.NoteResourceDataCacheImpl
import com.glitch.securenotes.data.cache.datacacheimpl.NotesDataCacheImpl
import com.glitch.securenotes.data.cache.datacacheimpl.UserCollectionsDataCacheImpl
import com.glitch.securenotes.data.cache.datacacheimpl.UsersDataCacheImpl
import org.koin.dsl.module

private const val MAX_CACHE_SIZE: Int = 1024

val cacheSystemsModule = module {
    single<UsersDataCache> {
        UsersDataCacheImpl(maxCacheSize = MAX_CACHE_SIZE)
    }
    single<NotesDataCache> {
        NotesDataCacheImpl(maxCacheSize = MAX_CACHE_SIZE)
    }
    single<NoteResourcesDataCache> {
        NoteResourceDataCacheImpl(maxCacheSize = MAX_CACHE_SIZE)
    }
    single<UserCollectionsDataCache> {
        UserCollectionsDataCacheImpl(maxCacheSize = MAX_CACHE_SIZE)
    }
}