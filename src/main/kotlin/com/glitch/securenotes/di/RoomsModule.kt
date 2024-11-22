package com.glitch.securenotes.di

import com.glitch.securenotes.domain.rooms.noteslist.UserNotesRoomController
import com.glitch.securenotes.domain.rooms.noteslist.UserNotesRoomControllerImpl
import org.koin.dsl.module

val roomsModule = module {

    single<UserNotesRoomController> {
        UserNotesRoomControllerImpl()
    }

}