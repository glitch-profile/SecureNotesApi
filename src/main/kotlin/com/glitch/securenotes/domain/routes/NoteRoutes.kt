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
import com.glitch.securenotes.domain.rooms.noteslist.NotesListSocketEvent
import com.glitch.securenotes.domain.rooms.noteslist.UserNotesRoomController
import com.glitch.securenotes.domain.sessions.AuthSession
import com.glitch.securenotes.domain.utils.ApiErrorCode
import com.glitch.securenotes.domain.utils.HeaderNames
import com.glitch.securenotes.domain.utils.UserRoleCode
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
    noteResourcesDataSource: NoteResourcesDataSource,
    notesRoomController: UserNotesRoomController
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
                val pagingOffset = call.queryParameters[HeaderNames.PAGING_PAGE]?.toIntOrNull() ?: 0
                val pagingLimit = call.queryParameters[HeaderNames.PAGING_LIMIT]?.toIntOrNull() ?: -1
                val secureNotesPassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]

                val excludedNoteIds = if ((user.protectedNoteIds.isNotEmpty()) && (user.protectedNotePassword != secureNotesPassword))
                    user.protectedNoteIds
                else emptySet()
                val notes = notesDataSource.getNotesForUserV2(
                    userId = session.userId,
                    page = pagingOffset,
                    limit = pagingLimit,
                    excludedNotesId = excludedNoteIds
                )
                val notesCompactInfo = notes.map { it.toCompactInfo(user.id) }
                call.respond(notesCompactInfo)
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
                notesRoomController.sendEventForUser(
                    userId = session.userId,
                    event = NotesListSocketEvent.NewNote(
                        initiatedUserId = session.userId,
                        newNoteInfoModel = addedNote.toCompactInfo(session.userId)
                    )
                )
                call.respond(
                    ApiResponseDto.Success(
                        data = addedNote.toCompactInfo(session.userId)
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
                notesRoomController.sendEventForUser(
                    userId = session.userId,
                    event = NotesListSocketEvent.NewNote(
                        initiatedUserId = session.userId,
                        newNoteInfoModel = addedNote.toCompactInfo(session.userId)
                    )
                )
                call.respond(
                    ApiResponseDto.Success(
                        data = addedNote.id
                    )
                )
            }

            route("/{${HeaderNames.NOTE_ID}}") {

                get {
                    val session = call.sessions.get<AuthSession>()!!
                    val noteId = call.request.pathVariables[HeaderNames.NOTE_ID]!!
                    val user = usersDataSource.getUserById(session.userId)
                    if (user.protectedNoteIds.contains(noteId)) {
                        val userSecuredPassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
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
                    val noteId = call.pathParameters[HeaderNames.NOTE_ID]!!
                    val note = notesDataSource.getNoteById(noteId = noteId, requestedUserId = session.userId)
                    if (note.creatorId == session.userId) {
                        noteResourcesDataSource.deleteResourceForNote(noteId = noteId, editorUserId = session.userId)
                    }
                    val result = notesDataSource.deleteNoteForUser(userId = session.userId, noteId = noteId)
                    if (result) {
                        val userIdsList = if (note.creatorId == session.userId) note.getAllUsers()
                        else listOf(session.userId)
                        notesRoomController.sendEventForUsers(
                            userIds = userIdsList,
                            event = NotesListSocketEvent.DeletedNote(
                                initiatedUserId = session.userId,
                                affectedNoteId = note.id
                            )
                        )
                    }
                    call.respond(
                        if (result) ApiResponseDto.Success(Unit)
                        else ApiResponseDto.Error()
                    )
                }

                post("/update-title") {
                    val session = call.sessions.get<AuthSession>()!!
                    val noteId = call.pathParameters[HeaderNames.NOTE_ID]!!
                    val newTitle = call.receiveText()
                    val user = usersDataSource.getUserById(userId = session.userId)
                    if (user.protectedNoteIds.contains(noteId)) {
                        val securedNotePassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
                        if (user.protectedNotePassword != securedNotePassword)
                            throw IncorrectSecuredNotesPasswordException()
                    }
                    val result = notesDataSource.updateNoteTitle(
                        noteId = noteId,
                        editorUserId = session.userId,
                        newTitle = newTitle
                    )
                    if (result) {
                        val note = notesDataSource.getNoteById(noteId, session.userId)
                        val noteUsers = note.getAllUsers()
                        notesRoomController.sendEventForUsers(
                            userIds = noteUsers,
                            event = NotesListSocketEvent.UpdatedNote(
                                initiatedUserId = session.userId,
                                updateNoteInfoModel = note.toCompactRoomSocketInfo()
                            )
                        )
                    }
                    call.respond(
                        if (result) ApiResponseDto.Success(Unit)
                        else ApiResponseDto.Error()
                    )
                }

                post("/update-description") {
                    val session = call.sessions.get<AuthSession>()!!
                    val noteId = call.pathParameters[HeaderNames.NOTE_ID]!!
                    val newDescription = call.receiveText()
                    val user = usersDataSource.getUserById(userId = session.userId)
                    if (user.protectedNoteIds.contains(noteId)) {
                        val securedNotePassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
                        if (user.protectedNotePassword != securedNotePassword)
                            throw IncorrectSecuredNotesPasswordException()
                    }
                    val result = notesDataSource.updateNoteDescription(
                        noteId = noteId,
                        editorUserId = session.userId,
                        newDescription = newDescription
                    )
                    if (result) {
                        val note = notesDataSource.getNoteById(noteId, session.userId)
                        val noteUsers = note.getAllUsers()
                        notesRoomController.sendEventForUsers(
                            userIds = noteUsers,
                            event = NotesListSocketEvent.UpdatedNote(
                                initiatedUserId = session.userId,
                                updateNoteInfoModel = note.toCompactRoomSocketInfo()
                            )
                        )
                    }
                    call.respond(
                        if (result) ApiResponseDto.Success(Unit)
                        else ApiResponseDto.Error()
                    )
                }

                // TODO: replace with noteRoomController later
                post("/update-text") {
                    val session = call.sessions.get<AuthSession>()!!
                    val noteId = call.pathParameters[HeaderNames.NOTE_ID]!!
                    val newText = call.receiveText()
                    val user = usersDataSource.getUserById(session.userId)
                    if (user.protectedNoteIds.contains(noteId)) {
                        val protectedNotesPassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
                        if (protectedNotesPassword != user.protectedNotePassword) throw IncorrectSecuredNotesPasswordException()
                    }
                    val result = notesDataSource.updateNoteText(
                        noteId = noteId,
                        editorUserId = session.userId,
                        newText = newText
                    )
                    if (result) {
                        val note = notesDataSource.getNoteById(noteId, session.userId)
                        val noteUsers = note.getAllUsers()
                        notesRoomController.sendEventForUsers(
                            userIds = noteUsers,
                            event = NotesListSocketEvent.UpdatedNote(
                                initiatedUserId = session.userId,
                                updateNoteInfoModel = note.toCompactRoomSocketInfo()
                            )
                        )
                    }
                    call.respond(
                        if (result) ApiResponseDto.Success(Unit)
                        else ApiResponseDto.Error()
                    )
                }

                route("/sharing") {

                    get("/users") {
                        val session = call.sessions.get<AuthSession>()!!
                        val noteId = call.pathParameters[HeaderNames.NOTE_ID]!!
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

                    put("/set-mode") {
                        val session = call.sessions.get<AuthSession>()!!
                        val noteId = call.pathParameters[HeaderNames.NOTE_ID]!!
                        val newNoteSharingPolicy = call.queryParameters[HeaderNames.NOTE_SHARING_MODE]
                        if (newNoteSharingPolicy != "shared" && newNoteSharingPolicy != "private") {
                            call.respond(HttpStatusCode.BadRequest)
                            return@put
                        }
                        val isShareNote = newNoteSharingPolicy == "shared"
                        val user = usersDataSource.getUserById(userId = session.userId)
                        if (user.protectedNoteIds.contains(noteId)) {
                            val securedNotePassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
                            if (user.protectedNotePassword != securedNotePassword)
                                throw IncorrectSecuredNotesPasswordException()
                        }
                        val result = if (isShareNote) {
                            notesDataSource.enableNoteSharing(noteId, session.userId)
                        } else {
                            val note = notesDataSource.getNoteById(noteId, session.userId)
                            val disableSharingResult = notesDataSource.disableNoteSharing(noteId, session.userId)
                            if (disableSharingResult) notesRoomController.sendEventForUsers(
                                userIds = note.getSharedUsers(),
                                event = NotesListSocketEvent.DeletedNote(
                                    initiatedUserId = session.userId,
                                    affectedNoteId = note.id
                                )
                            )
                            disableSharingResult
                        }
                        call.respond(
                            if (result) ApiResponseDto.Success(Unit)
                            else ApiResponseDto.Error()
                        )
                    }

                    put("/add-users") {
                        val session = call.sessions.get<AuthSession>()!!
                        val noteId = call.pathParameters[HeaderNames.NOTE_ID]!!
                        val userLists = call.receiveNullable<UserListsIncomingDto>() ?: kotlin.run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@put
                        }
                        val user = usersDataSource.getUserById(session.userId)
                        if (user.protectedNoteIds.contains(noteId)) {
                            val notePassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
                            if (user.protectedNotePassword != notePassword)
                                throw IncorrectSecuredNotesPasswordException()
                        }
                        val note = notesDataSource.getNoteById(noteId, session.userId)
                        val editorIdsToAdd = userLists.editors.take(10)
                        val readerIdsToAdd = userLists.readers.asSequence()
                            .filter { !editorIdsToAdd.contains(it) }
                            .take(10)
                            .toList()
                        if (editorIdsToAdd.isNotEmpty()) {
                            val foundedEditors = usersDataSource.getUsersByIds(editorIdsToAdd)
                                .map { it.id }
                                .toSet()
                            if (foundedEditors.isNotEmpty()) {
                                notesDataSource.addUsersToSharedEditorIds(
                                    noteId = noteId,
                                    requestedUserId = session.userId,
                                    userIds = foundedEditors
                                )
                                notesRoomController.sendEventForUsers(
                                    userIds = foundedEditors.toList(),
                                    event = NotesListSocketEvent.NewNote(
                                        initiatedUserId = session.userId,
                                        newNoteInfoModel = note.toCompactInfo(UserRoleCode.ROLE_EDITOR)
                                    )
                                )
                            }
                        }
                        if (readerIdsToAdd.isNotEmpty()) {
                            val foundedReaders = usersDataSource.getUsersByIds(readerIdsToAdd)
                                .map { it.id }
                                .toSet()
                            if (foundedReaders.isNotEmpty()) {
                                notesDataSource.addUsersToSharedReaderIds(
                                    noteId = noteId,
                                    requestedUserId = session.userId,
                                    userIds = foundedReaders
                                )
                                notesRoomController.sendEventForUsers(
                                    userIds = foundedReaders.toList(),
                                    event = NotesListSocketEvent.NewNote(
                                        initiatedUserId = session.userId,
                                        newNoteInfoModel = note.toCompactInfo(UserRoleCode.ROLE_READER)
                                    )
                                )
                            }
                        }
                        call.respond(
                            ApiResponseDto.Success(Unit)
                        )
                    }

                    put("/remove-users") {
                        val session = call.sessions.get<AuthSession>()!!
                        val noteId = call.pathParameters[HeaderNames.NOTE_ID]!!
                        val userLists = call.receiveNullable<UserListsIncomingDto>() ?: kotlin.run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@put
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
                            notesRoomController.sendEventForUsers(
                                userIds = editorIdsToRemove.toList(),
                                event = NotesListSocketEvent.DeletedNote(
                                    initiatedUserId = session.userId,
                                    affectedNoteId = noteId
                                )
                            )
                        }
                        if (readerIdsToRemove.isNotEmpty()) {
                            notesDataSource.removeUsersFromSharedReaderIds(
                                noteId = noteId,
                                requestedUserId = session.userId,
                                userIds = readerIdsToRemove
                            )
                            notesRoomController.sendEventForUsers(
                                userIds = readerIdsToRemove.toList(),
                                event = NotesListSocketEvent.DeletedNote(
                                    initiatedUserId = session.userId,
                                    affectedNoteId = noteId
                                )
                            )
                        }
                        call.respond(
                            ApiResponseDto.Success(Unit)
                        )
                    }

                    put("/remove-all") {
                        val session = call.sessions.get<AuthSession>()!!
                        val noteId = call.pathParameters[HeaderNames.NOTE_ID]!!
                        val result = notesDataSource.removeAllUsersFromSharedNote(noteId, session.userId)
                        if (result) {
                            val note = notesDataSource.getNoteById(noteId, session.userId)
                            notesRoomController.sendEventForUsers(
                                userIds = note.getSharedUsers(),
                                event = NotesListSocketEvent.DeletedNote(
                                    initiatedUserId = session.userId,
                                    affectedNoteId = noteId
                                )
                            )
                        }
                        call.respond(
                            if (result) ApiResponseDto.Success(Unit)
                            else ApiResponseDto.Error()
                        )
                    }

                    put("/update-owner") {
                        val session = call.sessions.get<AuthSession>()!!
                        val noteId = call.pathParameters[HeaderNames.NOTE_ID]!!
                        val editorUser = usersDataSource.getUserById(session.userId)
                        if (editorUser.protectedNoteIds.contains(noteId)) {
                            val protectedNotesPassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
                            if (protectedNotesPassword != editorUser.protectedNotePassword) {
                                throw IncorrectSecuredNotesPasswordException()
                            }
                        }
                        val newUserId = call.request.headers[HeaderNames.USER_ID] ?: kotlin.run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@put
                        }
                        val newUser = usersDataSource.getUserById(newUserId)
                        val result = notesDataSource.updateNoteOwner(
                            noteId = noteId,
                            requestedUserId = session.userId,
                            userId = newUser.id
                        )
                        if (result) {
                            notesRoomController.sendEventForUser(
                                userId = newUser.id,
                                event = NotesListSocketEvent.UpdatedRole(
                                    initiatedUserId = session.userId,
                                    affectedNoteId = noteId,
                                    newRoleCode = UserRoleCode.ROLE_OWNER
                                )
                            )
                            notesRoomController.sendEventForUser(
                                userId = editorUser.id,
                                event = NotesListSocketEvent.UpdatedRole(
                                    initiatedUserId = session.userId,
                                    affectedNoteId = noteId,
                                    newRoleCode = UserRoleCode.ROLE_EDITOR
                                )
                            )
                        }
                        call.respond(
                            if (result) ApiResponseDto.Success(Unit)
                            else ApiResponseDto.Error()
                        )
                    }

                    put("/move-to-editors") {
                        val session = call.sessions.get<AuthSession>()!!
                        val noteId = call.pathParameters[HeaderNames.NOTE_ID]!!
                        val editorUser = usersDataSource.getUserById(session.userId)
                        if (editorUser.protectedNoteIds.contains(noteId)) {
                            val protectedNotesPasswords = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
                            if (protectedNotesPasswords != editorUser.protectedNotePassword)
                                throw IncorrectSecuredNotesPasswordException()
                        }
                        val userList = call.receiveNullable<UserListsIncomingDto>()?.readers ?: kotlin.run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@put
                        }
                        val result = notesDataSource.moveUsersToEditors(
                            userIds = userList,
                            noteId = noteId,
                            requestedUserId = session.userId
                        )
                        if (result) {
                            notesRoomController.sendEventForUsers(
                                userIds = userList.toList(),
                                event = NotesListSocketEvent.UpdatedRole(
                                    initiatedUserId = session.userId,
                                    affectedNoteId = noteId,
                                    newRoleCode = UserRoleCode.ROLE_EDITOR
                                )
                            )
                        }
                        call.respond(
                            if (result) ApiResponseDto.Success(Unit)
                            else ApiResponseDto.Error()
                        )

                    }

                    put("/move-to-readers") {
                        val session = call.sessions.get<AuthSession>()!!
                        val noteId = call.pathParameters[HeaderNames.NOTE_ID]!!
                        val editorUser = usersDataSource.getUserById(session.userId)
                        if (editorUser.protectedNoteIds.contains(noteId)) {
                            val protectedNotesPassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
                            if (protectedNotesPassword != editorUser.protectedNotePassword) {
                                throw IncorrectSecuredNotesPasswordException()
                            }
                        }
                        val usersList = call.receiveNullable<UserListsIncomingDto>()?.editors ?: kotlin.run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@put
                        }
                        val note = notesDataSource.getNoteById(noteId, session.userId)
                        val usersToMove = note.sharedEditorUserIds.intersect(usersList)
                        val result = notesDataSource.moveUsersToReaders(
                            noteId = note.id,
                            requestedUserId = session.userId,
                            userIds = usersToMove
                        )
                        if (result) {
                            notesRoomController.sendEventForUsers(
                                userIds = usersToMove.toList(),
                                event = NotesListSocketEvent.UpdatedRole(
                                    initiatedUserId = session.userId,
                                    affectedNoteId = note.id,
                                    newRoleCode = UserRoleCode.ROLE_READER
                                )
                            )
                        }
                        call.respond(
                            if (result) ApiResponseDto.Success(Unit)
                            else ApiResponseDto.Error()
                        )
                    }

                }

                route("/protection") {

                    put("/add") {
                        val session = call.sessions.get<AuthSession>()!!
                        val noteId = call.pathParameters[HeaderNames.NOTE_ID]!!
                        val user = usersDataSource.getUserById(session.userId)
                        if (user.protectedNoteIds.contains(noteId)) {
                            call.respond(ApiResponseDto.Success(Unit))
                            return@put
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

                    put("/remove") {
                        val session = call.sessions.get<AuthSession>()!!
                        val noteId = call.pathParameters[HeaderNames.NOTE_ID]!!
                        val user = usersDataSource.getUserById(session.userId)
                        if (!user.protectedNoteIds.contains(noteId)) {
                            call.respond(ApiResponseDto.Success(Unit))
                            return@put
                        }
                        val notePassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
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

            delete {
                val session = call.sessions.get<AuthSession>()!!
                val noteIdsToDelete = call.receiveNullable<List<String>>() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest)
                    return@delete
                }
                val foundedNotesToDelete = notesDataSource.getNotesById(noteIdsToDelete.toSet(), session.userId)
                val userOwnedNoteIds = foundedNotesToDelete.asSequence()
                    .filter { it.creatorId == session.userId }
                    .map { it.id }
                    .toSet()
                noteResourcesDataSource.deleteResourceForNotes(userOwnedNoteIds, session.userId)
                val result = notesDataSource.deleteNotesForUser(
                    userId = session.userId,
                    noteIds = noteIdsToDelete.toSet()
                )
                call.respond(
                    if (result) ApiResponseDto.Success(Unit)
                    else ApiResponseDto.Error()
                )
            }

        }

    }
}