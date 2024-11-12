package com.glitch.securenotes.domain.routes

import com.glitch.securenotes.data.datasource.AuthSessionStorage
import com.glitch.securenotes.data.datasource.UsersDataSource
import com.glitch.securenotes.data.datasource.notes.NoteResourcesDataSource
import com.glitch.securenotes.data.datasource.notes.NotesDataSource
import com.glitch.securenotes.data.exceptions.users.UserNotFoundException
import com.glitch.securenotes.data.model.dto.ApiResponseDto
import com.glitch.securenotes.domain.plugins.AuthenticationLevel
import com.glitch.securenotes.domain.sessions.AuthSession
import com.glitch.securenotes.domain.utils.ApiErrorCode
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

fun Route.noteRoutes(
    usersDataSource: UsersDataSource,
    sessionStorage: AuthSessionStorage,
    notesDataSource: NotesDataSource,
    notesResourcesDataSource: NoteResourcesDataSource
) {

    route("/api/V1/notes") {

        authenticate(AuthenticationLevel.USER) {

            get {
                try {
                    val session = call.sessions.get<AuthSession>()!!
                    val user = usersDataSource.getUserById(session.userId)
                    val pagingOffset = call.queryParameters["offset"]?.toIntOrNull() ?: 0
                    val pagingLimit = call.queryParameters["limit"]?.toIntOrNull() ?: -1
                    val secureNotesPassword = call.request.headers["securedNotesPassword"]

                    val excludedNoteIds = if ((user.protectedNoteIds.isNotEmpty()) && (user.protectedNotePassword == secureNotesPassword))
                        user.protectedNoteIds
                    else emptySet()
                    val notes = notesDataSource.getNotesForUserV2(
                        userId = session.userId,
                        page = pagingOffset,
                        limit = pagingLimit,
                        excludedNotesId = excludedNoteIds
                    )
                    call.respond(notes)
                } catch (e: UserNotFoundException) {
                    sessionStorage.delete(call.sessionId<AuthSession>()!!)
                    call.respond(
                        ApiResponseDto.Error<Unit>(
                            apiErrorCode = ApiErrorCode.USER_NOT_FOUND,
                            message = ApiErrorCode::USER_NOT_FOUND.name
                        )
                    )
                }
            }

        }

    }
}