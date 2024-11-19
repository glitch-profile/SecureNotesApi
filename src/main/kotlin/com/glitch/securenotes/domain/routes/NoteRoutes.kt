package com.glitch.securenotes.domain.routes

import com.glitch.securenotes.data.datasource.UsersDataSource
import com.glitch.securenotes.data.datasource.notes.NoteResourcesDataSource
import com.glitch.securenotes.data.datasource.notes.NotesDataSource
import com.glitch.securenotes.data.exceptions.users.IncorrectSecuredNotesPasswordException
import com.glitch.securenotes.data.model.dto.ApiResponseDto
import com.glitch.securenotes.data.model.dto.notes.NewNoteIncomingInfoDto
import com.glitch.securenotes.data.model.dto.notes.NoteSharingStatusDto
import com.glitch.securenotes.data.model.dto.notes.UserListsIncomingDto
import com.glitch.securenotes.domain.plugins.AuthenticationLevel
import com.glitch.securenotes.domain.rooms.noteslist.UserNotesRoomController
import com.glitch.securenotes.domain.sessions.AuthSession
import com.glitch.securenotes.domain.utils.ApiErrorCode
import com.glitch.securenotes.domain.utils.HeaderNames
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.math.min

fun Route.noteRoutes(
    usersDataSource: UsersDataSource,
    notesDataSource: NotesDataSource,
    notesRoomController: UserNotesRoomController,
    noteResourcesDataSource: NoteResourcesDataSource
) {

    route("/api/V1/notes") {

        authenticate(AuthenticationLevel.USER) {

            webSocket {
                val session = call.sessions.get<AuthSession>()!!
                val user = usersDataSource.getUserById(session.userId)
                try {
                    notesRoomController.joinRoom(
                        userId = user.id,
                        userProtectedNotes = user.protectedNoteIds,
                        webSocketSession = this
                    )
                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            val command = frame.readText()
                            if (command == "hide-protected") {
                                notesRoomController.disableUpdatesForSecuredNotes(user.id)
                            } else if (command.startsWith("show-protected")) { // example: show-protected qwerty123
                                val currentUserPassword = usersDataSource.getUserById(user.id).protectedNotePassword
                                val enteredPassword = command.drop(15) //show-protected command prefix length
                                if (enteredPassword == currentUserPassword) notesRoomController.enableUpdatesForSecuredNotes(user.id)
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("NOTES ROUTES - LIST SOCKET - ERROR - ${e.stackTrace}")
                } finally {
                    notesRoomController.leaveRoom(user.id)
                }

            }

            get {
                val session = call.sessions.get<AuthSession>()!!
                val user = usersDataSource.getUserById(session.userId)
                val pagingOffset = call.queryParameters[HeaderNames.pagingPage]?.toIntOrNull() ?: 0
                val pagingLimit = call.queryParameters[HeaderNames.pagingLimit]?.toIntOrNull() ?: -1
                val secureNotesPassword = call.request.headers[HeaderNames.securedNotesPassword]

                val excludedNoteIds = if ((user.protectedNoteIds.isNotEmpty()) && (user.protectedNotePassword != secureNotesPassword))
                    user.protectedNoteIds
                else emptySet()
                val notes = notesDataSource.getNotesForUserV2(
                    userId = session.userId,
                    page = pagingOffset,
                    limit = pagingLimit,
                    excludedNotesId = excludedNoteIds
                )
                call.respond(notes)
            }

            post("/create") {
                val session = call.sessions.get<AuthSession>()!!
                val newNoteInfo = call.receiveNullable<NewNoteIncomingInfoDto>() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
                val formattedNoteInfo = newNoteInfo.copy(
                    title = if (newNoteInfo.title.isNullOrBlank()) null else newNoteInfo.title,
                    description = if (newNoteInfo.description.isNullOrBlank()) null else newNoteInfo.description,
                    text = newNoteInfo.text.trim(),
                    editorUserIds = if (newNoteInfo.isSharing) newNoteInfo.editorUserIds else emptySet(),
                    readerUserIds = if (newNoteInfo.isSharing) newNoteInfo.readerUserIds else emptySet()
                )
                val addedNote = notesDataSource.createNewNote(
                    creatorId = session.userId,
                    title = formattedNoteInfo.title,
                    description = formattedNoteInfo.description,
                    text = formattedNoteInfo.text,
                    isShared = formattedNoteInfo.isSharing,
                    sharedReaderUserIds = formattedNoteInfo.readerUserIds,
                    sharedEditorUserIds = formattedNoteInfo.editorUserIds
                )
                call.respond(
                    ApiResponseDto.Success(
                        data = addedNote
                    )
                )
            }

            post("/add-existing") {
                val session = call.sessions.get<AuthSession>()!!
                val newNoteInfo = call.receiveNullable<NewNoteIncomingInfoDto>() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
                val currentTimestamp = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond()
                val formattedNoteInfo = newNoteInfo.copy(
                    title = if (newNoteInfo.title.isNullOrBlank()) null else newNoteInfo.title,
                    description = if (newNoteInfo.description.isNullOrBlank()) null else newNoteInfo.description,
                    text = newNoteInfo.text.trim(),
                    editorUserIds = if (newNoteInfo.isSharing) newNoteInfo.editorUserIds else emptySet(),
                    readerUserIds = if (newNoteInfo.isSharing) newNoteInfo.readerUserIds else emptySet(),
                    createdTimestamp = newNoteInfo.createdTimestamp?.run { min(this, currentTimestamp) },
                    lastEditTimestamp = newNoteInfo.lastEditTimestamp?.run { min(this, currentTimestamp) }
                )
                val addedNote = notesDataSource.createNewNote(
                    creatorId = session.userId,
                    title = formattedNoteInfo.title,
                    description = formattedNoteInfo.description,
                    text = formattedNoteInfo.text,
                    isShared = formattedNoteInfo.isSharing,
                    sharedReaderUserIds = formattedNoteInfo.readerUserIds,
                    sharedEditorUserIds = formattedNoteInfo.editorUserIds,
                    createdTimestamp = formattedNoteInfo.createdTimestamp,
                    lastEditTimestamp = formattedNoteInfo.lastEditTimestamp
                )
                call.respond(
                    ApiResponseDto.Success(
                        data = addedNote.id
                    )
                )
            }

            route("/{${HeaderNames.noteId}}") {

                get {
                    val session = call.sessions.get<AuthSession>()!!
                    val noteId = call.request.pathVariables[HeaderNames.noteId] ?: kotlin.run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }
                    val userSecuredPassword = call.request.headers[HeaderNames.securedNotesPassword]
                    val user = usersDataSource.getUserById(session.userId)
                    if (user.protectedNoteIds.contains(noteId)) {
                        if (userSecuredPassword != user.protectedNotePassword) {
                            call.respond(
                                ApiResponseDto.Error<Unit>(
                                    apiErrorCode = ApiErrorCode.PROTECTED_NOTES_PASSWORD_INCORRECT,
                                    message = ApiErrorCode::PROTECTED_NOTES_PASSWORD_INCORRECT.name
                                )
                            )
                            return@get
                        }
                    }
                    val note = notesDataSource.getNoteById(noteId, user.id)
                    call.respond(
                        ApiResponseDto.Success(
                            data = note
                        )
                    )
                }

                delete {
                    val session = call.sessions.get<AuthSession>()!!
                    val noteId = call.pathParameters[HeaderNames.noteId] ?: kotlin.run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@delete
                    }
                    val note = notesDataSource.getNoteById(noteId = noteId, requestedUserId = session.userId)
                    if (note.creatorId == session.userId) {
                        noteResourcesDataSource.deleteResourceForNote(noteId = noteId, editorUserId = session.userId)
                    }
                    val result = notesDataSource.deleteNoteForUser(userId = session.userId, noteId = noteId)
                    if (result) {
                        call.respond(
                            ApiResponseDto.Success(
                                data = Unit
                            )
                        )
                    } else {
                        call.respond(
                            ApiResponseDto.Error<Unit>()
                        )
                    }
                }

                post("/update-title") {
                    val session = call.sessions.get<AuthSession>()!!
                    val noteId = call.pathParameters[HeaderNames.noteId] ?: kotlin.run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }
                    val securedNotePassword = call.request.headers[HeaderNames.securedNotesPassword]
                    val newTitle = call.receiveText()
                    val user = usersDataSource.getUserById(userId = session.userId)
                    if (user.protectedNoteIds.contains(noteId)) {
                        if (user.protectedNotePassword != securedNotePassword)
                            throw IncorrectSecuredNotesPasswordException()
                    }
                    val result = notesDataSource.updateNoteTitle(
                        noteId = noteId,
                        editorUserId = session.userId,
                        newTitle = newTitle
                    )
                    if (result) {
                        call.respond(
                            ApiResponseDto.Success(Unit)
                        )
                    } else {
                        call.respond(
                            ApiResponseDto.Error<Unit>()
                        )
                    }
                }

                post("/update-description") {
                    val session = call.sessions.get<AuthSession>()!!
                    val noteId = call.pathParameters[HeaderNames.noteId] ?: kotlin.run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }
                    val securedNotePassword = call.request.headers[HeaderNames.securedNotesPassword]
                    val newDescription = call.receiveText()
                    val user = usersDataSource.getUserById(userId = session.userId)
                    if (user.protectedNoteIds.contains(noteId)) {
                        if (user.protectedNotePassword != securedNotePassword)
                            throw IncorrectSecuredNotesPasswordException()
                    }
                    val result = notesDataSource.updateNoteDescription(
                        noteId = noteId,
                        editorUserId = session.userId,
                        newDescription = newDescription
                    )
                    if (result) {
                        call.respond(
                            ApiResponseDto.Success(Unit)
                        )
                    } else {
                        call.respond(
                            ApiResponseDto.Error<Unit>()
                        )
                    }
                }

                route("/sharing") {

                    get("/users") {
                        val session = call.sessions.get<AuthSession>()!!
                        val noteId = call.pathParameters[HeaderNames.noteId] ?: kotlin.run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@get
                        }
                        val note = notesDataSource.getNoteById(noteId, session.userId)
                        val sharingNoteStatus = NoteSharingStatusDto(
                            isSharing = note.isSharing,
                            creatorId = note.creatorId,
                            editorIds = note.sharedEditorUserIds,
                            readerIds = note.sharedReaderUserIds
                        )
                        call.respond(
                            ApiResponseDto.Success(data = sharingNoteStatus)
                        )
                    }

                    post("/set-mode") {
                        val session = call.sessions.get<AuthSession>()!!
                        val noteId = call.pathParameters[HeaderNames.noteId] ?: kotlin.run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@post
                        }
                        val newNoteSharingPolicy = call.queryParameters[HeaderNames.newNoteSharingMode]
                        if (newNoteSharingPolicy != "shared" && newNoteSharingPolicy != "private") {
                            call.respond(HttpStatusCode.BadRequest)
                            return@post
                        }
                        val isShareNote = newNoteSharingPolicy == "shared"
                        val securedNotePassword = call.request.headers[HeaderNames.securedNotesPassword]
                        val user = usersDataSource.getUserById(userId = session.userId)
                        if (user.protectedNoteIds.contains(noteId)) {
                            if (user.protectedNotePassword != securedNotePassword)
                                throw IncorrectSecuredNotesPasswordException()
                        }
                        val result = if (isShareNote) notesDataSource.enableNoteSharing(noteId, session.userId)
                        else notesDataSource.disableNoteSharing(noteId, session.userId)
                        call.respond(
                            if (result) ApiResponseDto.Success(Unit)
                            else ApiResponseDto.Error()
                        )
                    }

                    post("/add-users") {
                        val session = call.sessions.get<AuthSession>()!!
                        val noteId = call.pathParameters[HeaderNames.noteId] ?: kotlin.run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@post
                        }
                        val userLists = call.receiveNullable<UserListsIncomingDto>() ?: kotlin.run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@post
                        }
                        val notePassword = call.request.headers[HeaderNames.securedNotesPassword]
                        val user = usersDataSource.getUserById(session.userId)
                        if (user.protectedNoteIds.contains(noteId)) {
                            if (user.protectedNotePassword != notePassword)
                                throw IncorrectSecuredNotesPasswordException()
                        }
                        val editorIdsToAdd = userLists.editors.take(10)
                        val readerIdsToAdd = userLists.readers.asSequence()
                            .filter { !editorIdsToAdd.contains(it) }
                            .take(10)
                            .toList()
                        if (editorIdsToAdd.isNotEmpty()) {
                            val foundedEditors = usersDataSource.getUsersByIds(editorIdsToAdd)
                                .map { it.id }
                                .toSet()
                            if (foundedEditors.isNotEmpty()) notesDataSource.addUsersToSharedEditorIds(
                                noteId = noteId,
                                requestedUserId = session.userId,
                                userIds = foundedEditors
                            )
                        }
                        if (readerIdsToAdd.isNotEmpty()) {
                            val foundedReaders = usersDataSource.getUsersByIds(readerIdsToAdd)
                                .map { it.id }
                                .toSet()
                            if (foundedReaders.isNotEmpty()) notesDataSource.addUsersToSharedReaderIds(
                                noteId = noteId,
                                requestedUserId = session.userId,
                                userIds = foundedReaders
                            )
                        }
                        call.respond(
                            ApiResponseDto.Success(Unit)
                        )
                    }

                    post("/remove-users") {
                        val session = call.sessions.get<AuthSession>()!!
                        val noteId = call.pathParameters[HeaderNames.noteId] ?: kotlin.run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@post
                        }
                        val userLists = call.receiveNullable<UserListsIncomingDto>() ?: kotlin.run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@post
                        }
                        val editorIdsToRemove = userLists.editors.take(10).toSet()
                        val readerIdsToRemove = userLists.readers.asSequence()
                            .filter { !editorIdsToRemove.contains(it) }
                            .take(10)
                            .toSet()
                        if (editorIdsToRemove.isNotEmpty()) {
                            notesDataSource.removeUsersFromSharedEditorIds(
                                noteId = noteId,
                                requestedUserId = session.userId,
                                userIds = editorIdsToRemove
                            )
                        }
                        if (readerIdsToRemove.isNotEmpty()) {
                            notesDataSource.removeUsersFromSharedReaderIds(
                                noteId = noteId,
                                requestedUserId = session.userId,
                                userIds = readerIdsToRemove
                            )
                        }
                        call.respond(
                            ApiResponseDto.Success(Unit)
                        )
                    }

                    post("/remove-all") {
                        val session = call.sessions.get<AuthSession>()!!
                        val noteId = call.pathParameters[HeaderNames.noteId] ?: kotlin.run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@post
                        }
                        val result = notesDataSource.removeAllUsersFromSharedNote(noteId, session.userId)
                        call.respond(
                            if (result) ApiResponseDto.Success(Unit)
                            else ApiResponseDto.Error()
                        )
                    }

                    post("/update-owner") {
                        val session = call.sessions.get<AuthSession>()!!
                        val noteId = call.pathParameters[HeaderNames.noteId] ?: kotlin.run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@post
                        }
                        val protectedNotesPassword = call.request.headers[HeaderNames.securedNotesPassword]
                        val editorUser = usersDataSource.getUserById(session.userId)
                        if (editorUser.protectedNoteIds.contains(noteId)) {
                            if (protectedNotesPassword != editorUser.protectedNotePassword) {
                                throw IncorrectSecuredNotesPasswordException()
                            }
                        }
                        val newUserId = call.request.headers[HeaderNames.userId] ?: kotlin.run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@post
                        }
                        val newUser = usersDataSource.getUserById(newUserId)
                        val result = notesDataSource.updateNoteOwner(
                            noteId = noteId,
                            requestedUserId = session.userId,
                            userId = newUser.id
                        )
                        call.respond(
                            if (result) ApiResponseDto.Success(Unit)
                            else ApiResponseDto.Error()
                        )
                    }

                }

                route("/protection") {

                    post("/add") {
                        val session = call.sessions.get<AuthSession>()!!
                        val noteId = call.pathParameters[HeaderNames.noteId] ?: kotlin.run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@post
                        }
                        val user = usersDataSource.getUserById(session.userId)
                        if (user.protectedNoteIds.contains(noteId)) {
                            call.respond(ApiResponseDto.Success(Unit))
                            return@post
                        }
                        val result = usersDataSource.addNoteToProtected(session.userId, noteId)
                        if (result) {
                            notesRoomController.addNotesToProtected(
                                userId = user.id,
                                noteIds = setOf(noteId)
                            )
                        }
                        call.respond(
                            if (result) ApiResponseDto.Success(Unit)
                            else ApiResponseDto.Error()
                        )
                    }

                    post("/remove") {
                        val session = call.sessions.get<AuthSession>()!!
                        val noteId = call.pathParameters[HeaderNames.noteId] ?: kotlin.run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@post
                        }
                        val notePassword = call.request.headers[HeaderNames.securedNotesPassword]
                        val user = usersDataSource.getUserById(session.userId)
                        if (!user.protectedNoteIds.contains(noteId)) {
                            call.respond(ApiResponseDto.Success(Unit))
                            return@post
                        }
                        if (notePassword != user.protectedNotePassword)
                            throw IncorrectSecuredNotesPasswordException()
                        val result = usersDataSource.removeNoteFromProtected(
                            userId = session.userId,
                            noteId = noteId
                        )
                        if (result) {
                            notesRoomController.removeNotesFromProtected(
                                userId = user.id,
                                noteIds = setOf(noteId)
                            )
                        }
                        call.respond(
                            if (result) ApiResponseDto.Success(Unit)
                            else ApiResponseDto.Error()
                        )
                    }

                }

            }

        }

    }
}