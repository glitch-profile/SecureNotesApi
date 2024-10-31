package com.glitch.securenotes.di

import com.glitch.securenotes.data.cache.datacache.UsersDataCache
import com.glitch.securenotes.data.cache.datacacheimpl.UsersDataCacheImpl
import org.koin.dsl.module

private const val MAX_CACHE_SIZE: Int = 1024

val cacheSystemsModule = module {
    single<UsersDataCache> {
        UsersDataCacheImpl(maxCacheSize = MAX_CACHE_SIZE)
    }
}