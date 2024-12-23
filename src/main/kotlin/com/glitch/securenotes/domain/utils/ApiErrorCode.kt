package com.glitch.securenotes.domain.utils

object ApiErrorCode {
    // GENERAL ERROR CODES - 10X
    const val UNKNOWN_ERROR = 101
    const val NO_PERMISSIONS = 102
    // FILES ERROR CODES - 11X
    const val FILE_EXTENSION_UNKNOWN = 111
    // AUTH ERRORS CODES - 20X
    const val AUTH_DATA_INCORRECT = 201
    const val INCORRECT_PASSWORD_VERIFICATION = 202
    const val CREDENTIALS_ALREADY_IN_USE = 203
    const val AUTH_CODE_NOT_FOUND = 204
    const val AUTH_SESSION_NOT_FOUND = 205
    // USERS ERROR CODES - 21X
    const val USER_NOT_FOUND = 211
    const val PROTECTED_NOTES_PASSWORD_INCORRECT = 212
    // NOTES ERROR CODES - 22X
    const val NOTE_NOT_FOUND = 221
    const val NOTE_SHARING_NOT_CONFIGURED = 222

}