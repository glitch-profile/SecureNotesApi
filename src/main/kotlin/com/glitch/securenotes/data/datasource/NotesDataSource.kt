package com.glitch.securenotes.data.datasource

import com.glitch.securenotes.data.model.entity.NoteModel

interface NotesDataSource {

    suspend fun getNotesForUser(
        userId: String,
        page: Int = 0,
        limit: Int = -1
    ): List<NoteModel>

    suspend fun getProtectedNotesForUser(
        userId: String,
        securedNotesPassword: String,
        page: Int = 0,
        limit: Int = -1
    ): List<NoteModel>

    suspend fun getOneNoteById(noteId: String): NoteModel

    suspend fun getManyNotesById(noteIds: List<String>): List<NoteModel>

    suspend fun deleteOneNoteById(noteId: String)

    suspend fun deleteManyNotesById(noteIds: List<String>)

    suspend fun deleteAllNotesForUser(userId: String)

}