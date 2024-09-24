package com.glitch.securenotes.data.model.dto

import kotlinx.serialization.Serializable

@Serializable
sealed class ApiResponseDto<T>(
    val data: T?,
    val status: Boolean,
    val apiErrorCode: Int? = null,
    val message: String
) {
    class Success<T>(
        data: T?,
        message: String = "Ok",
        apiErrorCode: Int? = null,
    ): ApiResponseDto<T>(
        data = data,
        status = true,
        apiErrorCode = apiErrorCode,
        message = message
    )
    class Error<T>(
        apiErrorCode: Int,
        message: String,
        data: T? = null
    ): ApiResponseDto<T>(
        data = data,
        status = false,
        apiErrorCode = apiErrorCode,
        message = message
    )
}
