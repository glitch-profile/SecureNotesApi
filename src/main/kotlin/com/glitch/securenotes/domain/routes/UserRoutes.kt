package com.glitch.securenotes.domain.routes

import com.glitch.floweryapi.domain.utils.encryptor.AESEncryptor
import com.glitch.securenotes.data.datasource.AuthSessionStorage
import com.glitch.securenotes.data.datasource.UserCredentialsDataSource
import com.glitch.securenotes.data.datasource.UsersDataSource
import com.glitch.securenotes.data.exceptions.auth.CredentialsNotFoundException
import com.glitch.securenotes.data.exceptions.auth.IncorrectCredentialsException
import com.glitch.securenotes.data.exceptions.auth.SessionNotFoundException
import com.glitch.securenotes.data.exceptions.users.UnknownAvatarFormatException
import com.glitch.securenotes.data.exceptions.users.UserNotFoundException
import com.glitch.securenotes.data.model.dto.ApiResponseDto
import com.glitch.securenotes.data.model.dto.auth.AuthSessionOutgoingDto
import com.glitch.securenotes.data.model.dto.users.UserInfoDto
import com.glitch.securenotes.data.model.dto.users.UserUpdatePasswordDto
import com.glitch.securenotes.data.model.entity.FileModel
import com.glitch.securenotes.domain.sessions.AuthSession
import com.glitch.securenotes.domain.utils.ApiErrorCode
import com.glitch.securenotes.domain.utils.filemanager.FileManager
import com.glitch.securenotes.domain.utils.imageprocessor.ImageProcessor
import com.glitch.securenotes.domain.utils.imageprocessor.ImageProcessorConstants
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.utils.io.*
import kotlinx.io.readByteArray
import java.io.File

// max new avatar image size
private const val MAX_FILE_SIZE = 5_242_880 // 5 MB in bytes

fun Route.userRoutes(
    usersDataSource: UsersDataSource,
    userCredentialsDataSource: UserCredentialsDataSource,
    authSessionStorage: AuthSessionStorage,
    fileManager: FileManager,
    imageProcessor: ImageProcessor
) {

    route("api/V1/users") {

        authenticate("guest") {

            put("/update-avatar") {
                val session = call.sessions.get<AuthSession>()!!
                val contentLength = call.request.header(HttpHeaders.ContentLength)?.toInt() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest)
                    return@put
                }
                if (contentLength > MAX_FILE_SIZE) {
                    call.respond(HttpStatusCode.PayloadTooLarge)
                    return@put
                }
                try {
                    val multipartData = call.receiveMultipart()
                    val user = usersDataSource.getUserById(session.userId) // making sure this user is existing
                    var newAvatarImageInfo: FileModel? = null
                    val encryptor = AESEncryptor
                    multipartData.forEachPart { part ->
                        if (newAvatarImageInfo == null) {
                            when (part) {
                                is PartData.FormItem -> Unit
                                is PartData.FileItem -> {
                                    val fileName = part.originalFileName!!.filter {
                                        it.isLetterOrDigit() || it == '.' || it == '_'
                                    }
                                    if (!imageProcessor.isImage(fileName)) {
                                        throw UnknownAvatarFormatException()
                                    }
                                    val fileBytes = part.provider().readRemaining().readByteArray()

                                    // image without compression
                                    val imageExtension = File(fileName).extension
                                    val uploadedFileLocalPath = fileManager.uploadTempFile(
                                        fileBytes = fileBytes,
                                        fileExtension = imageExtension
                                    )
                                    val originalFile = File(uploadedFileLocalPath)

                                    // thumbnail image
                                    val thumbnailImage = File(fileManager.generateFilePath("jpg"))
                                    imageProcessor.compressImageAndCrop(
                                        inputFile = originalFile,
                                        outputFile = thumbnailImage,
                                        sideSize = ImageProcessorConstants.AVATAR_PREVIEW,
                                        compressionQuality = ImageProcessorConstants.COMPRESSION_MODE_LOW_QUALITY
                                    )
                                    // thumbnail image encryption
                                    val thumbnailImageBytes = thumbnailImage.inputStream().use { it.readBytes() }
                                    thumbnailImage.outputStream().use { it.write(encryptor.encrypt(thumbnailImageBytes)) }

                                    // default processed image
                                    val defaultImage = File(fileManager.generateFilePath("jpg"))
                                    imageProcessor.compressImageAndCrop(
                                        inputFile = originalFile,
                                        outputFile = defaultImage,
                                        sideSize = ImageProcessorConstants.AVATAR_DEFAULT,
                                        compressionQuality = ImageProcessorConstants.COMPRESSION_MODE_HIGH_QUALITY
                                    )
                                    // default image encryption
                                    val defaultImageBytes = defaultImage.inputStream().use { it.readBytes() }
                                    defaultImage.outputStream().use { it.write(encryptor.encrypt(defaultImageBytes)) }

                                    val imageInfo = FileModel(
                                        name = fileName,
                                        urlPath = fileManager.toUrlPath(defaultImage.path),
                                        previewUrlPath = fileManager.toUrlPath(thumbnailImage.path)
                                    )
                                    newAvatarImageInfo = imageInfo
                                    originalFile.delete()
                                }
                                else -> Unit
                            }
                            part.dispose()
                        }
                    }
                    if (user.profileAvatar != null) { // deleting old avatar
                        val defaultAvatarPath = fileManager.toLocalPath(user.profileAvatar.urlPath)
                        val avatarThumbnailPath = fileManager.toLocalPath(user.profileAvatar.previewUrlPath!!)
                        kotlin.runCatching { fileManager.deleteFile(defaultAvatarPath) }
                        kotlin.runCatching { fileManager.deleteFile(avatarThumbnailPath) }
                    }
                    if (newAvatarImageInfo != null) {
                        usersDataSource.updateUserProfileAvatar(
                            userId = user.id,
                            avatarUrlPath = newAvatarImageInfo!!.urlPath,
                            avatarThumbnailUrlPath = newAvatarImageInfo!!.previewUrlPath!!
                        )
                    } else {
                        usersDataSource.clearUserProfileAvatar(user.id)
                    }
                    call.respond(
                        ApiResponseDto.Success(
                            data = null,
                            message = "Avatar updated"
                        )
                    )
                } catch (e: UnknownAvatarFormatException) {
                    call.respond(
                        ApiResponseDto.Error<Unit>(
                            apiErrorCode = ApiErrorCode.FILE_EXTENSION_UNKNOWN,
                            message = "This file format is not supported by this endpoint"
                        )
                    )
                } catch (e: UserNotFoundException) {
                    call.respond(
                        ApiResponseDto.Error<Unit>(
                            apiErrorCode = ApiErrorCode.USER_NOT_FOUND,
                            message = "User not found"
                        )
                    )
                }
            }

            route("/sync") {

                post("/enable") {
                    val session = call.sessions.get<AuthSession>()!!
                    val newEncryptionCode = AESEncryptor.generateSecret()
                    try {
                        val result = usersDataSource.enableEncryptionKeySync(
                            userId = session.userId,
                            encryptionKey = newEncryptionCode
                        )
                        if (result) {
                            call.respond(
                                ApiResponseDto.Success(
                                    data = null,
                                    message = "Sync enabled"
                                )
                            )
                        } else {
                            call.respond(
                                ApiResponseDto.Error<Unit>()
                            )
                        }
                    } catch (e: UserNotFoundException) {
                        call.respond(
                            ApiResponseDto.Error<Unit>(
                                apiErrorCode = ApiErrorCode.USER_NOT_FOUND,
                                message = "User not found"
                            )
                        )
                    }
                }

                post("/disable") {
                    val session = call.sessions.get<AuthSession>()!!
                    try {
                        val result = usersDataSource.disableEncryptionKeySync(
                            userId = session.userId
                        )
                        if (result) {
                            call.respond(
                                ApiResponseDto.Success(
                                    data = null,
                                    message = "Sync enabled"
                                )
                            )
                        } else {
                            call.respond(
                                ApiResponseDto.Error<Unit>()
                            )
                        }
                    } catch (e: UserNotFoundException) {
                        call.respond(
                            ApiResponseDto.Error<Unit>(
                                apiErrorCode = ApiErrorCode.USER_NOT_FOUND,
                                message = "User not found"
                            )
                        )
                    }
                }

            }

            get("/{user_id}") {
                val session = call.sessions.get<AuthSession>()!!
                val requestedUserId = call.request.pathVariables["user_id"] ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                try {
                    val requestedUser = usersDataSource.getUserById(requestedUserId)
                    if (session.userId == requestedUserId) {
                        val encryptionKey = if (requestedUser.syncedEncryptionKey != null) {
                            AESEncryptor.decrypt(requestedUser.syncedEncryptionKey)
                        } else null
                        call.respond(
                            ApiResponseDto.Success(
                                data = UserInfoDto(
                                    id = requestedUser.id,
                                    username = requestedUser.username,
                                    encryptionKey = encryptionKey,
                                    profileImage = requestedUser.profileAvatar,
                                    accountCreationTimestamp = requestedUser.creationDate
                                )
                            )
                        )
                    } else {
                        call.respond(
                            ApiResponseDto.Success(
                                data = UserInfoDto(
                                    id = requestedUser.id,
                                    username = requestedUser.username,
                                    profileImage = requestedUser.profileAvatar
                                )
                            )
                        )
                    }
                } catch (e: UserNotFoundException) {
                    call.respond(
                        ApiResponseDto.Error<Unit>(
                            apiErrorCode = ApiErrorCode.USER_NOT_FOUND,
                            message = "User not found"
                        )
                    )
                }
            }

            put("/update-username") {
                val session = call.sessions.get<AuthSession>()!!
                val userName = call.receiveText()
                val userNameFormatted = userName.filterNot {
                    it.isLetterOrDigit() || it in "~_-+=*#@!<>,./?"
                }.take(20)
                try {
                    if (userNameFormatted.isNotBlank() && userNameFormatted.length >= 3) {
                        val result = usersDataSource.updateUsername(
                            userId = session.userId,
                            newUsername = userNameFormatted
                        )
                        if (result) {
                            call.respond(
                                ApiResponseDto.Success(
                                    data = null,
                                    message = "Username updated"
                                )
                            )
                        } else {
                            call.respond(ApiResponseDto.Error<Unit>())
                        }
                    } else call.respond(HttpStatusCode.BadRequest)
                } catch (e: UserNotFoundException) {
                    call.respond(
                        ApiResponseDto.Error<Unit>(
                            apiErrorCode = ApiErrorCode.USER_NOT_FOUND,
                            message = "User not found"
                        )
                    )
                }
            }

            put("/update-password") {
                val session = call.sessions.get<AuthSession>()!!
                val passwordData = call.receiveNullable<UserUpdatePasswordDto>() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest)
                    return@put
                }
                try {
                    val newPasswordFormatted = passwordData.newPassword.filterNot {
                        it.isISOControl() || it.isWhitespace()
                    }.take(20)
                    val result = userCredentialsDataSource.updateCredentials(
                        userId = session.userId,
                        oldPassword = passwordData.oldPassword,
                        newPassword = newPasswordFormatted
                    )
                    if (result) {
                        call.respond(
                            ApiResponseDto.Success(
                                data = null,
                                message = "Password updated"
                            )
                        )
                    } else {
                        call.respond(
                            ApiResponseDto.Error<Unit>()
                        )
                    }
                } catch (e: UserNotFoundException) {
                    call.respond(
                        ApiResponseDto.Error<Unit>(
                            apiErrorCode = ApiErrorCode.USER_NOT_FOUND,
                            message = "User not found"
                        )
                    )
                } catch (e: CredentialsNotFoundException) {
                    call.respond(
                        ApiResponseDto.Error<Unit>(
                            apiErrorCode = ApiErrorCode.USER_NOT_FOUND,
                            message = "User not found"
                        )
                    )
                } catch (e: IncorrectCredentialsException) {
                    call.respond(
                        ApiResponseDto.Error<Unit>(
                            apiErrorCode = ApiErrorCode.AUTH_DATA_INCORRECT,
                            message = "Incorrect password"
                        )
                    )
                }
            }

            delete("/delete") {
                val session = call.sessions.get<AuthSession>()!!
                val userId = session.userId
                try {
                    val user = usersDataSource.getUserById(userId)
                    user.activeSessions.forEach { sessionId ->
                        authSessionStorage.delete(sessionId)
                    }
                    // TODO: delete all notes for user
                    val result = usersDataSource.deleteUserById(userId)
                    if (result) {
                        call.respond(
                            ApiResponseDto.Success(
                                data = null,
                                message = "User deleted. We will miss you"
                            )
                        )
                    } else {
                        call.respond(
                            ApiResponseDto.Error<Unit>()
                        )
                    }
                } catch (e: UserNotFoundException) {
                    call.respond(
                        ApiResponseDto.Error<Unit>(
                            apiErrorCode = ApiErrorCode.USER_NOT_FOUND,
                            message = "This user not found"
                        )
                    )
                }
            }

            route("/sessions") {

                get {
                    val session = call.sessions.get<AuthSession>()!!
                    try {
                        val activeSessionIds = usersDataSource.getUserById(session.userId).activeSessions
                        val activeSessions = activeSessionIds.mapNotNull { id ->
                            try {
                                val sessionData = authSessionStorage.get(id)
                                AuthSessionOutgoingDto(
                                    id = id,
                                    platform = sessionData.platformName,
                                    agentName = sessionData.agentName,
                                    lastActiveTimestamp = sessionData.lastActivityTimestamp
                                )
                            } catch (e: SessionNotFoundException) {
                                null
                            }
                        }.sortedByDescending { it.lastActiveTimestamp }
                        call.respond(
                            ApiResponseDto.Success(
                                data = activeSessions
                            )
                        )
                    } catch (e: UserNotFoundException) {
                        call.respond(
                            ApiResponseDto.Error<Unit>(
                                apiErrorCode = ApiErrorCode.USER_NOT_FOUND,
                                message = "User not found"
                            )
                        )
                    }
                }

                post("/{sessionId}/destroy") {
                    val session = call.sessions.get<AuthSession>()!!
                    val sessionId = call.pathParameters["sessionId"] ?: kotlin.run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }
                    try {
                        val sessionToClose = authSessionStorage.get(sessionId)
                        if (session.userId == sessionToClose.userId) {
                            usersDataSource.removeActiveSessionId(
                                userId = session.userId,
                                sessionId = sessionId
                            )
                            authSessionStorage.delete(sessionId)
                            call.respond(
                                ApiResponseDto.Success(
                                    data = null
                                )
                            )
                        } else {
                            call.respond(HttpStatusCode.Forbidden)
                        }
                    } catch (e: SessionNotFoundException) {
                        call.respond(HttpStatusCode.NotFound)
                    }
                    // TODO: Replace with api error codes
                }

                post("/destroy") {
                    val session = call.sessions.get<AuthSession>()!!
                    val isIncludeClientSession = call.request.queryParameters["include-self"].toBoolean()
                    try {
                        val userInfo = usersDataSource.getUserById(session.userId)
                        val sessionIdsToDelete = if (!isIncludeClientSession) {
                            userInfo.activeSessions.toMutableList().apply {
                                remove(call.sessionId<AuthSession>())
                            }
                        } else userInfo.activeSessions
                        sessionIdsToDelete.forEach { id ->
                            authSessionStorage.delete(id)
                        }
                        val result = usersDataSource.removeActiveSessionId(
                            userId = session.userId,
                            sessionsIds = sessionIdsToDelete
                        )
                        if (result) {
                            call.respond(
                                ApiResponseDto.Success(
                                    data = null
                                )
                            )
                        } else {
                            call.respond(
                                ApiResponseDto.Error<Unit>()
                            )
                        }
                    } catch (e: UserNotFoundException) {
                        call.respond(
                            ApiResponseDto.Error<Unit>(
                                apiErrorCode = ApiErrorCode.USER_NOT_FOUND,
                                message = "User not found"
                            )
                        )
                    }
                }

            }

        }

    }

}