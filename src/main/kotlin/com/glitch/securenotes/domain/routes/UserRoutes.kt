package com.glitch.securenotes.domain.routes

import com.glitch.securenotes.data.datasource.UserCredentialsDataSource
import com.glitch.securenotes.data.datasource.UsersDataSource
import com.glitch.securenotes.data.exceptions.users.UnknownAvatarFormatException
import com.glitch.securenotes.data.model.entity.FileModel
import com.glitch.securenotes.domain.sessions.AuthSession
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
private val MAX_FILE_SIZE = 5_242_880 // 5 MB in bytes

fun Route.userRoutes(
    usersDataSource: UsersDataSource,
    userCredentialsDataSource: UserCredentialsDataSource,
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

    }

}