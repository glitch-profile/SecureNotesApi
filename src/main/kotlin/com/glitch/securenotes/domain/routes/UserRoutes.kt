package com.glitch.securenotes.domain.routes

import com.glitch.floweryapi.domain.utils.encryptor.AESEncryptor
import com.glitch.securenotes.data.datasource.AuthSessionStorage
import com.glitch.securenotes.data.datasource.UserCredentialsDataSource
import com.glitch.securenotes.data.datasource.UsersDataSource
import com.glitch.securenotes.data.exceptions.users.UnknownAvatarFormatException
import com.glitch.securenotes.data.exceptions.users.UserNotFoundException
import com.glitch.securenotes.data.model.dto.ApiResponseDto
import com.glitch.securenotes.data.model.dto.users.UserInfoDto
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

            post("/update-avatar") {
                val session = call.sessions.get<AuthSession>()!!
                val contentLength = call.request.header(HttpHeaders.ContentLength)?.toInt() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
                if (contentLength > MAX_FILE_SIZE) {
                    call.respond(HttpStatusCode.PayloadTooLarge)
                    return@post
                }
                try {
                    val multipartData = call.receiveMultipart()
                    var newAvatarImageInfo: FileModel? = null
                    multipartData.forEachPart { part ->
                        if (newAvatarImageInfo == null) {
                            when (part) {
                                is PartData.FormItem -> { }
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
                                    val uploadedFileLocalPath = fileManager.uploadFile(
                                        fileExtension = imageExtension,
                                        fileNameSuffix = "-t",
                                        fileBytes = fileBytes
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

                                    val defaultImage = File(fileManager.generateFilePath("jpg"))
                                    imageProcessor.compressImageAndCrop(
                                        inputFile = originalFile,
                                        outputFile = defaultImage,
                                        sideSize = ImageProcessorConstants.AVATAR_DEFAULT,
                                        compressionQuality = ImageProcessorConstants.COMPRESSION_MODE_HIGH_QUALITY
                                    )

                                    val imageInfo = FileModel(
                                        name = fileName,
                                        urlPath = fileManager.toUrlPath(defaultImage.path),
                                        previewUrlPath = fileManager.toUrlPath(thumbnailImage.path)
                                    )
                                    newAvatarImageInfo = imageInfo
                                    originalFile.delete()
                                }
                                else -> { }
                            }
                            part.dispose()
                        }
                    }
                    if (newAvatarImageInfo != null) {
                        usersDataSource.updateUserProfileAvatar(
                            userId = session.userId,
                            imageInfo = newAvatarImageInfo
                        )
                    } else call.respond(HttpStatusCode.BadRequest)
                } catch (e: UnknownAvatarFormatException) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                } catch (e: Exception) {
                    e.printStackTrace()
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
                        ApiResponseDto.Error<Unit>(
                            apiErrorCode = ApiErrorCode.UNKNOWN_ERROR,
                            message = "Sorry, we are unable to delete this user"
                        )
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

    }

}