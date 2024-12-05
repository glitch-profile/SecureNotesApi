package com.glitch.securenotes.domain.routes

import com.glitch.securenotes.data.datasource.UserCollectionsDataSource
import com.glitch.securenotes.data.datasource.notes.NotesDataSource
import com.glitch.securenotes.data.model.dto.ApiResponseDto
import com.glitch.securenotes.data.model.dto.collections.NewIncomingNoteCollectionDto
import com.glitch.securenotes.domain.plugins.AuthenticationLevel
import com.glitch.securenotes.domain.sessions.AuthSession
import com.glitch.securenotes.domain.utils.HeaderNames
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

fun Route.collectionRoutes(
    notesDataSource: NotesDataSource,
    collectionsDataSource: UserCollectionsDataSource
) {

    route("api/V1/collections") {

        authenticate(AuthenticationLevel.USER) {

            get {
                val session = call.sessions.get<AuthSession>()!!
                val collectionIds = call.receiveNullable<Set<String>>() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                val collections = collectionsDataSource.getCollectionsByIds(collectionIds, session.userId)
                call.respond(ApiResponseDto.Success(collections))
            }

            get("/all") {
                val session = call.sessions.get<AuthSession>()!!
                val collections = collectionsDataSource.getCollectionForUser(session.userId)
                call.respond(ApiResponseDto.Success(collections))
            }

            // TODO: really should init assigned note Ids on collection creation?
            post {
                val session = call.sessions.get<AuthSession>()!!
                val collectionData = call.receiveNullable<NewIncomingNoteCollectionDto>() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
                val createdCollection = collectionsDataSource.addCollection(
                    title = collectionData.title.trim().take(100),
                    description = collectionData.description?.trim()?.take(250),
                    assignedNoteIds = collectionData.assignedNoteIds,
                    userId = session.userId
                )
                call.respond(ApiResponseDto.Success(createdCollection))
            }

            route("/${HeaderNames.COLLECTION_ID}") {

                get {
                    val session = call.sessions.get<AuthSession>()!!
                    val collectionId = call.pathParameters[HeaderNames.COLLECTION_ID]!!
                    val collection = collectionsDataSource.getCollectionById(collectionId, session.userId)
                    call.respond(ApiResponseDto.Success(collection))
                }

                put("/update-title") {
                    val session = call.sessions.get<AuthSession>()!!
                    val collectionId = call.pathParameters[HeaderNames.COLLECTION_ID]!!
                    val title = call.receiveText().trim()
                    if (title.isEmpty()) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@put
                    }
                    val result = collectionsDataSource.updateCollectionTitle(
                        collectionId = collectionId,
                        userId = session.userId,
                        newTitle = title.take(100)
                    )
                    call.respond(
                        if (result) ApiResponseDto.Success(Unit)
                        else ApiResponseDto.Error()
                    )
                }

                put("/update-description") {
                    val session = call.sessions.get<AuthSession>()!!
                    val collectionId = call.pathParameters[HeaderNames.COLLECTION_ID]!!
                    val description = call.receiveText().trim()
                    val result = collectionsDataSource.updateCollectionDescription(
                        collectionId = collectionId,
                        userId = session.userId,
                        newDescription = if (description.isEmpty()) null else description.take(250)
                    )
                    call.respond(
                        if (result) ApiResponseDto.Success(Unit)
                        else ApiResponseDto.Error()
                    )
                }

                put("/add-notes") {
                    val session = call.sessions.get<AuthSession>()!!
                    val collectionId = call.pathParameters[HeaderNames.COLLECTION_ID]!!
                    val noteIds = call.receiveNullable<Set<String>>() ?: kotlin.run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@put
                    }
                    val result = collectionsDataSource.addNotesToCollection(
                        collectionId = collectionId,
                        userId = session.userId,
                        noteIds = noteIds
                    )
                    call.respond(
                        if (result) ApiResponseDto.Success(Unit)
                        else ApiResponseDto.Error()
                    )
                }

                put("/remove-notes") {
                    val session = call.sessions.get<AuthSession>()!!
                    val collectionId = call.pathParameters[HeaderNames.COLLECTION_ID]!!
                    val noteIds = call.receiveNullable<Set<String>>() ?: kotlin.run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@put
                    }
                    val result = collectionsDataSource.removeNotesFromCollection(
                        collectionId = collectionId,
                        userId = session.userId,
                        noteIds = noteIds
                    )
                    call.respond(
                        if (result) ApiResponseDto.Success(Unit)
                        else ApiResponseDto.Error()
                    )
                }

                delete {
                    val session = call.sessions.get<AuthSession>()!!
                    val collectionId = call.pathParameters[HeaderNames.COLLECTION_ID]!!
                    val result = collectionsDataSource.deleteCollectionById(collectionId, session.userId)
                    call.respond(
                        if (result) ApiResponseDto.Success(Unit)
                        else ApiResponseDto.Error()
                    )
                }

            }

            delete {
                val session = call.sessions.get<AuthSession>()!!
                val collectionIds = call.receiveNullable<Set<String>>() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest)
                    return@delete
                }
                val result = collectionsDataSource.deleteCollectionByIds(collectionIds, session.userId)
                call.respond(
                    if (result) ApiResponseDto.Success(Unit)
                    else ApiResponseDto.Error()
                )
            }

        }


    }

}