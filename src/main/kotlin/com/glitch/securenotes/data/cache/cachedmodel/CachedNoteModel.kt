package com.glitch.securenotes.data.cache.cachedmodel

import com.glitch.securenotes.data.model.entity.NoteModel

data class CachedNoteModel(
    val noteModel: NoteModel,
    val lastUsedTimestamp: Long
)