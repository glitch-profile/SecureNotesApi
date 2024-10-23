package com.glitch.securenotes.domain.routes

import com.glitch.securenotes.data.datasource.notes.NoteResourcesDataSource
import com.glitch.securenotes.data.datasource.notes.NotesDataSource
import io.ktor.server.routing.*

fun Route.noteRoutes(
    notesDataSource: NotesDataSource,
    notesResourcesDataSource: NoteResourcesDataSource
) {

}