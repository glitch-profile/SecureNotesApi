package com.glitch.securenotes.domain.routes

import com.glitch.securenotes.data.datasource.UsersDataSource
import com.glitch.securenotes.data.datasource.notes.NoteResourcesDataSource
import com.glitch.securenotes.data.datasource.notes.NotesDataSource
import com.glitch.securenotes.domain.plugins.AuthenticationLevel
import com.glitch.securenotes.domain.utils.HeaderNames
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.resourceRoutes(
    usersDataSource: UsersDataSource,
    notesDataSource: NotesDataSource,
    noteResourcesDataSource: NoteResourcesDataSource
) {

    route("api/V1/resources") {

        authenticate(AuthenticationLevel.USER) {

        }

    }

    route("api/V1/notes/{${HeaderNames.noteId}}/resources") {

        authenticate(AuthenticationLevel.USER) {

        }

    }
}