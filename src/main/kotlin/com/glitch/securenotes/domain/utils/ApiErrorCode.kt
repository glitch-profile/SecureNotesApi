package com.glitch.securenotes.domain.utils

object ApiErrorCode {
    // GENERAL ERROR CODES - 10X
    const val UNKNOWN_ERROR = 101
    const val NO_PERMISSIONS = 102
    const val USER_NOT_FOUND = 103
    // FILES ERROR CODES - 11X
    const val FILE_EXTENSION_UNKNOWN = 111
    // AUTH ERRORS CODES - 20X
    const val AUTH_DATA_INCORRECT = 201
    const val CREDENTIALS_ALREADY_IN_USE = 202
    const val AUTH_CODE_NOT_FOUND = 203
    const val AUTH_SESSION_NOT_FOUND = 204
    // USERS ERROR CODES - 21X
    const val PROTECTED_NOTES_PASSWORD_INCORRECT = 211

}