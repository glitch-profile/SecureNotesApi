package com.glitch.securenotes.data.cache.datacacheimpl

import com.glitch.securenotes.data.cache.cachedmodel.CachedNoteModel
import com.glitch.securenotes.data.cache.datacache.NotesDataCache
import com.glitch.securenotes.data.model.entity.NoteModel
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

class NotesDataCacheImpl(
    private val maxCacheSize: Int
): NotesDataCache {

    private val notes = ConcurrentHashMap<String, CachedNoteModel>()

    override fun getNoteById(noteId: String): NoteModel? {
        if (notes.containsKey(noteId)) {
            val info = notes[noteId]!!
            notes[noteId] = info.copy(
                lastUsedTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            )
            return info.noteModel
        }
        else return null
    }

    override fun getNotesByIds(noteIds: List<String>): List<NoteModel> {
        val foundedNotes = mutableListOf<NoteModel>()
        noteIds.forEach {
            val noteInfo = notes[it]
            if (noteInfo != null) {
                foundedNotes.add(noteInfo.noteModel)
                notes[it] = noteInfo.copy(
                    lastUsedTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
                )
            }
        }
        return foundedNotes.toList()
    }

    override fun isNoteSaved(noteId: String): Boolean {
        return notes.containsKey(noteId)
    }

    override fun addNoteToCache(noteModel: NoteModel) {
        if (!isNoteSaved(noteModel.id)) {
            if (notes.size >= maxCacheSize) {
                val oldestNote = notes.minByOrNull { it.value.lastUsedTimestamp }!!
                deleteNoteById(oldestNote.key)
            }
            notes[noteModel.id] = CachedNoteModel(
                noteModel = noteModel,
                lastUsedTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            )
        }
    }

    override fun updateSavedNote(noteModel: NoteModel) {
        if (isNoteSaved(noteModel.id)) {
            notes[noteModel.id] = CachedNoteModel(
                noteModel = noteModel,
                lastUsedTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            )
        }
    }

    override fun updateSavedNoteOrAdd(noteModel: NoteModel) {
        if (isNoteSaved(noteModel.id)) {
            notes[noteModel.id] = CachedNoteModel(
                noteModel = noteModel,
                lastUsedTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
            )
        } else addNoteToCache(noteModel)
    }

    override fun updateSavedNotesIfExists(noteModels: List<NoteModel>) {
        noteModels.forEach {
            if (isNoteSaved(it.id)) {
                notes[it.id] = CachedNoteModel(
                    noteModel = it,
                    lastUsedTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
                )
            }
        }
    }

    override fun deleteNoteById(noteId: String) {
        notes.remove(noteId)
    }

    override fun deleteNotesByIds(noteIds: List<String>) {
        noteIds.forEach { deleteNoteById(it) }
    }
}