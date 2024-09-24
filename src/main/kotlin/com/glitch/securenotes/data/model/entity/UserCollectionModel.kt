package com.glitch.securenotes.data.model.entity

import org.bson.codecs.pojo.annotations.BsonId

// TODO: Add collections implementation
data class UserCollectionModel(
    @BsonId
    val id: String = BsonId().toString(),
    val title: String,
    val description: String?
)
