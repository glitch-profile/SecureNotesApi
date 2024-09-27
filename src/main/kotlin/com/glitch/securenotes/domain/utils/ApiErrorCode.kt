package com.glitch.securenotes.domain.utils

object ApiErrorCode {
    // GENERAL ERROR CODES - 100
    const val UNKNOWN_ERROR = 101
    const val USER_NOT_FOUND = 102
    // AUTH ERRORS CODES - 200
    const val AUTH_DATA_INCORRECT = 201
    const val CREDENTIALS_ALREADY_IN_USE = 202
    const val AUTH_CODE_NOT_FOUND = 203

}