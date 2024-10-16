package com.glitch.securenotes.data.model.dto

import com.glitch.securenotes.domain.utils.ApiErrorCode
import kotlinx.serialization.Serializable

@Serializable
sealed class ApiResponseDto<T> {
    abstract val data: T?
    abstract val status: Boolean
    abstract val apiErrorCode: Int?
    abstract val message: String

    @Serializable
    data class Success<T>(
        override val data: T?,
        override val message: String = "Ok",
        override val apiErrorCode: Int? = null,
    ): ApiResponseDto<T>() {
        override val status = true
    }

    @Serializable
    data class Error<T>(
        override val apiErrorCode: Int = ApiErrorCode.UNKNOWN_ERROR,
        override val message: String = ApiErrorCode::UNKNOWN_ERROR.name,
        override val data: T? = null
    ): ApiResponseDto<T>() {
        override val status = false
    }
}
