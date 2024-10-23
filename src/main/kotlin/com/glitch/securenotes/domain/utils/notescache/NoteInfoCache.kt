package com.glitch.securenotes.domain.utils.notescache

import com.glitch.securenotes.data.model.entity.FileModel
import java.time.OffsetDateTime
import java.time.ZoneId

data class NoteInfoCache(
    val creatorId: String,
    val editorUserIds: Set<String>,
    val readerUserIds: Set<String>,
    val noteEncryptionKey: String,
    val noteResource: List<FileModel>,
    val cacheLastActiveTimestamp: Long = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
)
