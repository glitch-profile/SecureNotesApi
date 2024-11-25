package com.glitch.securenotes.domain.routes

import com.glitch.floweryapi.domain.utils.encryptor.AESEncryptor
import com.glitch.securenotes.data.datasource.UsersDataSource
import com.glitch.securenotes.data.datasource.notes.NoteResourcesDataSource
import com.glitch.securenotes.data.datasource.notes.NotesDataSource
import com.glitch.securenotes.data.exceptions.users.IncorrectSecuredNotesPasswordException
import com.glitch.securenotes.data.model.dto.ApiResponseDto
import com.glitch.securenotes.domain.plugins.AuthenticationLevel
import com.glitch.securenotes.domain.sessions.AuthSession
import com.glitch.securenotes.domain.utils.HeaderNames
import com.glitch.securenotes.domain.utils.filemanager.FileManager
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

fun Route.resourceRoutes(
    usersDataSource: UsersDataSource,
    notesDataSource: NotesDataSource,
    noteResourcesDataSource: NoteResourcesDataSource,
    fileManager: FileManager
) {

    route("api/V1/resources") {

        authenticate(AuthenticationLevel.USER) {

        }

    }

    route("api/V1/notes/{${HeaderNames.NOTE_ID}}/resources") {

        authenticate(AuthenticationLevel.USER) {

            route("/{${HeaderNames.RESOURCE_ID}}") {

                get {
                    val session = call.sessions.get<AuthSession>()!!
                    val noteId = call.pathParameters[HeaderNames.NOTE_ID] ?: kotlin.run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }
                    val resourceId = call.pathParameters[HeaderNames.RESOURCE_ID] ?: kotlin.run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }
                    val user = usersDataSource.getUserById(session.userId)
                    if (user.protectedNoteIds.contains(noteId)) {
                        val protectedNotePassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
                        if (user.protectedNotePassword != protectedNotePassword)
                            throw IncorrectSecuredNotesPasswordException()
                    }
                    val resource = noteResourcesDataSource.getResourceById(
                        noteId = noteId,
                        resourceId = resourceId,
                        requestedUserId = user.id
                    )
                    call.respond(
                        ApiResponseDto.Success(data = resource)
                    )
                }

                get("/file") {
                    val session = call.sessions.get<AuthSession>()!!
                    val noteId = call.pathParameters[HeaderNames.NOTE_ID] ?: kotlin.run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }
                    val resourceId = call.pathParameters[HeaderNames.RESOURCE_ID] ?: kotlin.run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }
                    val user = usersDataSource.getUserById(session.userId)
                    if (user.protectedNoteIds.contains(noteId)) {
                        val protectedNotePassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
                        if (user.protectedNotePassword != protectedNotePassword)
                            throw IncorrectSecuredNotesPasswordException()
                    }
                    val note = notesDataSource.getNoteById(
                        noteId = noteId,
                        requestedUserId = user.id
                    )
                    val resource = noteResourcesDataSource.getResourceById(
                        noteId = noteId,
                        resourceId = resourceId,
                        requestedUserId = user.id
                    )
                    val file = fileManager.getFile(fileManager.toLocalPath(resource.file.urlPath))
                    val fileBytes = file.inputStream().use { it.readBytes() }
                    val decryptedFileBytes = AESEncryptor.decrypt(fileBytes, note.encryptionKey)
                    call.response.header(
                        HttpHeaders.ContentType,
                        ContentType.defaultForFile(file).toString()
                    )
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Inline.withParameter(
                            ContentDisposition.Parameters.FileName, resource.file.name
                        ).toString()
                    )
                    call.respondBytes(decryptedFileBytes)
                }

                get("/preview") {
                    val session = call.sessions.get<AuthSession>()!!
                    val noteId = call.pathParameters[HeaderNames.NOTE_ID] ?: kotlin.run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }
                    val resourceId = call.pathParameters[HeaderNames.RESOURCE_ID] ?: kotlin.run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }
                    val user = usersDataSource.getUserById(session.userId)
                    if (user.protectedNoteIds.contains(noteId)) {
                        val protectedNotePassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
                        if (user.protectedNotePassword != protectedNotePassword)
                            throw IncorrectSecuredNotesPasswordException()
                    }
                    val note = notesDataSource.getNoteById(
                        noteId = noteId,
                        requestedUserId = user.id
                    )
                    val resource = noteResourcesDataSource.getResourceById(
                        noteId = noteId,
                        resourceId = resourceId,
                        requestedUserId = user.id
                    )
                    if (resource.file.previewUrlPath == null) {
                        call.respond(HttpStatusCode.NotFound)
                        return@get
                    }
                    val file = fileManager.getFile(fileManager.toLocalPath(resource.file.previewUrlPath))
                    val fileBytes = file.inputStream().use { it.readBytes() }
                    val decryptedFileBytes = AESEncryptor.decrypt(fileBytes, note.encryptionKey)
                    call.response.header(
                        HttpHeaders.ContentType,
                        ContentType.defaultForFile(file).toString()
                    )
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Inline.withParameter(
                            ContentDisposition.Parameters.FileName, "${resource.file.name}-preview"
                        ).toString()
                    )
                    call.respondBytes(decryptedFileBytes)
                }

                post("update-title") {
                    val session = call.sessions.get<AuthSession>()!!
                    val noteId = call.pathParameters[HeaderNames.NOTE_ID] ?: kotlin.run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }
                    val resourceId = call.pathParameters[HeaderNames.RESOURCE_ID] ?: kotlin.run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }
                    val newResourceTitle = call.receiveText()
                    val user = usersDataSource.getUserById(session.userId)
                    if (user.protectedNoteIds.contains(noteId)) {
                        val protectedNotePassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
                        if (user.protectedNotePassword != protectedNotePassword)
                            throw IncorrectSecuredNotesPasswordException()
                    }
                    val result = noteResourcesDataSource.updateResourceTitle(
                        noteId = noteId,
                        resourceId = resourceId,
                        editorUserId = user.id,
                        newTitle = newResourceTitle
                    )
                    call.respond(
                        if (result) ApiResponseDto.Success(Unit)
                        else ApiResponseDto.Error()
                    )
                }

                post("update-description") {
                    val session = call.sessions.get<AuthSession>()!!
                    val noteId = call.pathParameters[HeaderNames.NOTE_ID] ?: kotlin.run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }
                    val resourceId = call.pathParameters[HeaderNames.RESOURCE_ID] ?: kotlin.run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }
                    val newResourceDescription = call.receiveText()
                    val user = usersDataSource.getUserById(session.userId)
                    if (user.protectedNoteIds.contains(noteId)) {
                        val protectedNotePassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
                        if (user.protectedNotePassword != protectedNotePassword)
                            throw IncorrectSecuredNotesPasswordException()
                    }
                    val result = noteResourcesDataSource.updateResourceDescription(
                        noteId = noteId,
                        resourceId = resourceId,
                        editorUserId = user.id,
                        newDescription = newResourceDescription
                    )
                    call.respond(
                        if (result) ApiResponseDto.Success(Unit)
                        else ApiResponseDto.Error()
                    )
                }

                delete {
                    val session = call.sessions.get<AuthSession>()!!
                    val noteId = call.pathParameters[HeaderNames.NOTE_ID] ?: kotlin.run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@delete
                    }
                    val resourceId = call.pathParameters[HeaderNames.RESOURCE_ID] ?: kotlin.run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@delete
                    }
                    val user = usersDataSource.getUserById(session.userId)
                    if (user.protectedNoteIds.contains(noteId)) {
                        val protectedNotePassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
                        if (user.protectedNotePassword != protectedNotePassword)
                            throw IncorrectSecuredNotesPasswordException()
                    }
                    val result = noteResourcesDataSource.deleteResourceById(
                        noteId = noteId,
                        editorUserId = user.id,
                        resourceId = resourceId
                    )
                    call.respond(
                        if (result) ApiResponseDto.Success(Unit)
                        else ApiResponseDto.Error()
                    )
                }

            }

        }

    }
}