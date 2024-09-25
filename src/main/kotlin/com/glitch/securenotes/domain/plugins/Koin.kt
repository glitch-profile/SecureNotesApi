package com.glitch.securenotes.domain.plugins

import com.glitch.securenotes.di.dataSourceModule
import com.glitch.securenotes.di.databaseModule
import com.glitch.securenotes.di.utilsModule
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin

fun Application.configureKoin() {
    install(Koin) {
        modules(
            databaseModule,
            dataSourceModule,
            utilsModule
        )
    }
}