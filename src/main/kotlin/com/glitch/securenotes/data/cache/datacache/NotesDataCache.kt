package com.glitch.securenotes.data.cache.datacache

import com.glitch.securenotes.data.model.entity.NoteModel

interface NotesDataCache {

    fun getNoteById(noteId: String): NoteModel?

    /**
     * IMPORTANT
     *
     * This method returns all notes that were found in the cache.
     * It is possible that only part of the given list of note IDs may be returned
     */
    fun getNotesByIds(noteIds: List<String>): List<NoteModel>

    fun isNoteSaved(noteId: String): Boolean

    fun addNoteToCache(noteModel: NoteModel)

    fun updateSavedNote(noteModel: NoteModel)

    fun updateSavedNotesIfExists(noteModels: List<NoteModel>)

    fun deleteNoteById(noteId: String)

    fun deleteNotesByIds(noteIds: List<String>)

}