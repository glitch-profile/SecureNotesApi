package com.glitch.securenotes.domain.plugins

import com.glitch.securenotes.data.exceptions.auth.CredentialsNotFoundException
import com.glitch.securenotes.data.exceptions.auth.IncorrectCredentialsException
import com.glitch.securenotes.data.exceptions.auth.LoginAlreadyInUseException
import com.glitch.securenotes.data.exceptions.auth.SessionNotFoundException
import com.glitch.securenotes.data.exceptions.notes.BaseNoPermissionException
import com.glitch.securenotes.data.exceptions.notes.NoteNotConfiguredForSharingException
import com.glitch.securenotes.data.exceptions.notes.NoteNotFoundException
import com.glitch.securenotes.data.exceptions.resources.ResourceNotFoundException
import com.glitch.securenotes.data.exceptions.usercollections.CollectionNotFoundException
import com.glitch.securenotes.data.exceptions.users.IncorrectSecuredNotesPasswordException
import com.glitch.securenotes.data.exceptions.users.ProtectedNotesNotConfiguredException
import com.glitch.securenotes.data.exceptions.users.UserNotFoundException
import com.glitch.securenotes.data.model.dto.ApiResponseDto
import com.glitch.securenotes.domain.utils.ApiErrorCode
import com.glitch.securenotes.domain.utils.filemanager.UnknownExtensionException
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call: ApplicationCall, cause: Throwable ->
            when (cause) {
                // AUTH EXCEPTIONS
                is CredentialsNotFoundException -> call.respond(
                    ApiResponseDto.Error<Unit>(
                        apiErrorCode = ApiErrorCode.AUTH_DATA_INCORRECT,
                        message = "credentials not found"
                    )
                )
                is IncorrectCredentialsException -> call.respond(
                    ApiResponseDto.Error<Unit>(
                        apiErrorCode = ApiErrorCode.INCORRECT_CREDENTIALS,
                        message = "incorrect user credentials"
                    )
                )
                is LoginAlreadyInUseException -> call.respond(
                    ApiResponseDto.Error<Unit>(
                        apiErrorCode = ApiErrorCode.CREDENTIALS_ALREADY_IN_USE,
                        message = "this login is already in use"
                    )
                )
                is SessionNotFoundException -> call.respond(
                    ApiResponseDto.Error<Unit>(
                        apiErrorCode = ApiErrorCode.AUTH_SESSION_NOT_FOUND,
                        message = "session not found. Please login again"
                    )
                )
                // NOTES EXCEPTIONS
                is NoteNotFoundException -> call.respond(
                    ApiResponseDto.Error<Unit>(
                        apiErrorCode = ApiErrorCode.NOTE_NOT_FOUND,
                        message = "note not found"
                    )
                )
                is BaseNoPermissionException -> call.respond(
                    ApiResponseDto.Error<Unit>(
                        apiErrorCode = ApiErrorCode.NO_PERMISSIONS,
                        message = "no permissions to access this note"
                    )
                )
                is NoteNotConfiguredForSharingException -> call.respond(
                    ApiResponseDto.Error<Unit>(
                        apiErrorCode = ApiErrorCode.NOTE_SHARING_NOT_CONFIGURED,
                        message = "this note is not configured for sharing"
                    )
                )
                // RESOURCE EXCEPTIONS
                is ResourceNotFoundException -> call.respond(
                    ApiResponseDto.Error<Unit>(
                        apiErrorCode = TODO(),
                        message = "resource not found"
                    )
                )
                // USER COLLECTIONS EXCEPTIONS
                is CollectionNotFoundException -> call.respond(
                    ApiResponseDto.Error<Unit>(
                        apiErrorCode = TODO(),
                        message = "resource not found"
                    )
                )
                // USERS EXCEPTIONS
                is UserNotFoundException -> call.respond(
                    ApiResponseDto.Error<Unit>(
                        apiErrorCode = ApiErrorCode.USER_NOT_FOUND,
                        message = "user not found"
                    )
                )
                is IncorrectSecuredNotesPasswordException -> call.respond(
                    ApiResponseDto.Error<Unit>(
                        apiErrorCode = ApiErrorCode.PROTECTED_NOTES_PASSWORD_INCORRECT,
                        message = "incorrect password for protected notes"
                    )
                )
                is ProtectedNotesNotConfiguredException -> call.respond(
                    ApiResponseDto.Error<Unit>(
                        apiErrorCode = TODO(),
                        message = "protected notes password is not configured"
                    )
                )
                // UTILS EXTENSIONS
                is UnknownExtensionException -> call.respond(
                    ApiResponseDto.Error<Unit>(
                        apiErrorCode = ApiErrorCode.FILE_EXTENSION_UNKNOWN,
                        message = "this file format is not supported by this endpoint"
                    )
                )
                // OTHER
                else -> call.respond( ApiResponseDto.Error<Unit>() )
            }
        }
    }
}