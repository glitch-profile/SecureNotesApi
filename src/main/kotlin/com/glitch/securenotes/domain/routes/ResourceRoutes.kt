package com.glitch.securenotes.domain.routes

import com.glitch.floweryapi.domain.utils.encryptor.AESEncryptor
import com.glitch.securenotes.data.datasource.UsersDataSource
import com.glitch.securenotes.data.datasource.notes.NoteResourcesDataSource
import com.glitch.securenotes.data.datasource.notes.NotesDataSource
import com.glitch.securenotes.data.exceptions.users.IncorrectSecuredNotesPasswordException
import com.glitch.securenotes.data.model.dto.ApiResponseDto
import com.glitch.securenotes.data.model.entity.FileModel
import com.glitch.securenotes.data.model.entity.ResourceModel
import com.glitch.securenotes.domain.plugins.AuthenticationLevel
import com.glitch.securenotes.domain.sessions.AuthSession
import com.glitch.securenotes.domain.utils.HeaderNames
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

private const val MAX_CONTENT_LENGTH = 20_971_520L // 20 MB in bytes

fun Route.resourceRoutes(
    usersDataSource: UsersDataSource,
    notesDataSource: NotesDataSource,
    noteResourcesDataSource: NoteResourcesDataSource,
    fileManager: FileManager,
    imageProcessor: ImageProcessor
) {

    get("/api/V1/resource-file/{${HeaderNames.NOTE_ID}}/{${HeaderNames.FILE_PATH}}") {
        val session = call.sessions.get<AuthSession>()!!
        val noteId = call.pathParameters[HeaderNames.NOTE_ID]!!
        val filePath = call.queryParameters[HeaderNames.FILE_PATH]!!
        val user = usersDataSource.getUserById(session.userId)
        if (user.protectedNoteIds.contains(noteId)) {
            val protectedNotePassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
            if (user.protectedNotePassword != protectedNotePassword)
                throw IncorrectSecuredNotesPasswordException()
        }
        val note = notesDataSource.getNoteById(noteId, user.id)
        val file = fileManager.getFile(fileManager.toLocalPath(filePath))
        val decryptedFileBytes = AESEncryptor.decrypt(
            file.inputStream().use { it.readBytes() },
            note.encryptionKey
        )
        call.response.header(
            HttpHeaders.ContentType,
            ContentType.defaultForFile(file).toString()
        )
        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Inline.withParameter(
                ContentDisposition.Parameters.FileName, filePath
            ).toString()
        )
        call.respondBytes(decryptedFileBytes)

    }

    route("/api/V1/notes/{${HeaderNames.NOTE_ID}}/resources") {

        authenticate(AuthenticationLevel.USER) {

            get {
                val session = call.sessions.get<AuthSession>()!!
                val noteId = call.pathParameters[HeaderNames.NOTE_ID]!!
                val user = usersDataSource.getUserById(session.userId)
                if (user.protectedNoteIds.contains(noteId)) {
                    val protectedNotePassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
                    if (user.protectedNotePassword != protectedNotePassword)
                        throw IncorrectSecuredNotesPasswordException()
                }
                val resourcesForNote = noteResourcesDataSource.getResourcesForNote(noteId, user.id)
                call.respond(
                    ApiResponseDto.Success(resourcesForNote)
                )
            }

            post("/add") {
                val session = call.sessions.get<AuthSession>()!!
                val noteId = call.pathParameters[HeaderNames.NOTE_ID]!!
                val contentLength = call.request.header(HttpHeaders.ContentLength)?.toLong() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
                if (contentLength > MAX_CONTENT_LENGTH) {
                    call.respond(HttpStatusCode.PayloadTooLarge)
                    return@post
                }
                val note = notesDataSource.getNoteById(noteId, session.userId)
                val multipartData = call.receiveMultipart()
                var resourceFile: FileModel? = null
                var resourceTitle: String? = null
                var resourceDescription: String? = null
                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            if (resourceFile == null) {
                                val fileName = part.originalFileName!!.filter {
                                    it.isLetterOrDigit() || it == '.' || it == '_'
                                }
                                if (imageProcessor.isImage(fileName)) {
                                    // temp file
                                    val tempFilePath = fileManager.uploadTempFile(
                                        fileBytes = part.provider().readRemaining().readByteArray(),
                                        fileExtension = File(fileName).extension
                                    )
                                    val originalFile = File(tempFilePath)
                                    // thumbnail image
                                    val thumbnailImage = File(fileManager.generateFilePath("jpg"))
                                    imageProcessor.compressImageAndCrop(
                                        inputFile = originalFile,
                                        outputFile = thumbnailImage,
                                        sideSize = ImageProcessorConstants.RECT_MAX_IMAGE_THUMBNAIL,
                                        compressionQuality = ImageProcessorConstants.COMPRESSION_MODE_LOW_QUALITY
                                    )
                                    // thumbnail image encryption
                                    val thumbnailImageBytes = thumbnailImage.inputStream().use { it.readBytes() }
                                    thumbnailImage.outputStream().use { it.write(AESEncryptor.encrypt(thumbnailImageBytes)) }
                                    // default processed image
                                    val defaultImage = File(fileManager.generateFilePath("jpg"))
                                    imageProcessor.compressImageAndCrop(
                                        inputFile = originalFile,
                                        outputFile = defaultImage,
                                        sideSize = ImageProcessorConstants.RECT_MAX_IMAGE_DEFAULT,
                                        compressionQuality = ImageProcessorConstants.COMPRESSION_MODE_HIGH_QUALITY
                                    )
                                    // default image encryption
                                    val defaultImageBytes = defaultImage.inputStream().use { it.readBytes() }
                                    defaultImage.outputStream().use { it.write(AESEncryptor.encrypt(defaultImageBytes)) }
                                    resourceFile = FileModel(
                                        name = fileName,
                                        urlPath = fileManager.toUrlPath(defaultImage.path),
                                        previewUrlPath = fileManager.toUrlPath(thumbnailImage.path)
                                    )
                                    originalFile.delete()
                                } else {
                                    // file with encryption
                                    val encryptedFileBytes = AESEncryptor.encrypt(
                                        normalByteArray = part.provider().readRemaining().readByteArray(),
                                        secretKey = note.encryptionKey)
                                    val fileExtension = File(fileName).extension
                                    val uploadedFilePath = fileManager.uploadFile(encryptedFileBytes, fileExtension)
                                    resourceFile = FileModel(
                                        name = fileName,
                                        urlPath = fileManager.toUrlPath(uploadedFilePath)
                                    )
                                }
                            }
                        }
                        is PartData.FormItem -> {
                            when (part.name!!) {
                                "title" -> resourceTitle = part.value
                                "description" -> resourceDescription = part.value
                                else -> Unit
                            }
                        }
                        else -> Unit
                    }
                    part.dispose()
                }
                if (resourceFile != null) {
                    val result = noteResourcesDataSource.addResourceForNote(
                        noteId = noteId,
                        editorUserId = session.userId,
                        title = resourceTitle ?: resourceFile!!.name,
                        description = resourceDescription,
                        fileModel = resourceFile!!
                    )
                    call.respond(
                        ApiResponseDto.Success(data = result)
                    )
                    return@post
                }
                call.respond(
                    ApiResponseDto.Error<ResourceModel>()
                )
            }

            route("/{${HeaderNames.RESOURCE_ID}}") {

                get {
                    val session = call.sessions.get<AuthSession>()!!
                    val noteId = call.pathParameters[HeaderNames.NOTE_ID]!!
                    val resourceId = call.pathParameters[HeaderNames.RESOURCE_ID]!!
                    val user = usersDataSource.getUserById(session.userId)
                    if (user.protectedNoteIds.contains(noteId)) {
                        val protectedNotePassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
                        if (user.protectedNotePassword != protectedNotePassword)
                            throw IncorrectSecuredNotesPasswordException()
                    }
                    val resource = noteResourcesDataSource.getResourceById(
                        noteId = noteId,
                        resourceId = resourceId,
                        requestedUserId = user.id
                    )
                    call.respond(
                        ApiResponseDto.Success(data = resource)
                    )
                }

                get("/download") {
                    val session = call.sessions.get<AuthSession>()!!
                    val noteId = call.pathParameters[HeaderNames.NOTE_ID]!!
                    val resourceId = call.pathParameters[HeaderNames.RESOURCE_ID]!!
                    val user = usersDataSource.getUserById(session.userId)
                    if (user.protectedNoteIds.contains(noteId)) {
                        val protectedNotePassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
                        if (user.protectedNotePassword != protectedNotePassword)
                            throw IncorrectSecuredNotesPasswordException()
                    }
                    val note = notesDataSource.getNoteById(
                        noteId = noteId,
                        requestedUserId = user.id
                    )
                    val resource = noteResourcesDataSource.getResourceById(
                        noteId = noteId,
                        resourceId = resourceId,
                        requestedUserId = user.id
                    )
                    val file = fileManager.getFile(fileManager.toLocalPath(resource.file.urlPath))
                    val fileBytes = file.inputStream().use { it.readBytes() }
                    val decryptedFileBytes = AESEncryptor.decrypt(fileBytes, note.encryptionKey)
                    call.response.header(
                        HttpHeaders.ContentType,
                        ContentType.defaultForFile(file).toString()
                    )
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.File.withParameter(
                            ContentDisposition.Parameters.FileName, resource.file.name
                        ).toString()
                    )
                    call.respondBytes(decryptedFileBytes)
                }

                put("update-title") {
                    val session = call.sessions.get<AuthSession>()!!
                    val noteId = call.pathParameters[HeaderNames.NOTE_ID]!!
                    val resourceId = call.pathParameters[HeaderNames.RESOURCE_ID]!!
                    val newResourceTitle = call.receiveText()
//                    val user = usersDataSource.getUserById(session.userId)
//                    if (user.protectedNoteIds.contains(noteId)) {
//                        val protectedNotePassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
//                        if (user.protectedNotePassword != protectedNotePassword)
//                            throw IncorrectSecuredNotesPasswordException()
//                    }
                    val result = noteResourcesDataSource.updateResourceTitle(
                        noteId = noteId,
                        resourceId = resourceId,
                        editorUserId = session.userId,
                        newTitle = newResourceTitle
                    )
                    call.respond(
                        if (result) ApiResponseDto.Success(Unit)
                        else ApiResponseDto.Error()
                    )
                }

                put("update-description") {
                    val session = call.sessions.get<AuthSession>()!!
                    val noteId = call.pathParameters[HeaderNames.NOTE_ID]!!
                    val resourceId = call.pathParameters[HeaderNames.RESOURCE_ID]!!
                    val newResourceDescription = call.receiveText()
//                    val user = usersDataSource.getUserById(session.userId)
//                    if (user.protectedNoteIds.contains(noteId)) {
//                        val protectedNotePassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
//                        if (user.protectedNotePassword != protectedNotePassword)
//                            throw IncorrectSecuredNotesPasswordException()
//                    }
                    val result = noteResourcesDataSource.updateResourceDescription(
                        noteId = noteId,
                        resourceId = resourceId,
                        editorUserId = session.userId,
                        newDescription = newResourceDescription
                    )
                    call.respond(
                        if (result) ApiResponseDto.Success(Unit)
                        else ApiResponseDto.Error()
                    )
                }

                delete {
                    val session = call.sessions.get<AuthSession>()!!
                    val noteId = call.pathParameters[HeaderNames.NOTE_ID]!!
                    val resourceId = call.pathParameters[HeaderNames.RESOURCE_ID]!!
//                    val user = usersDataSource.getUserById(session.userId)
//                    if (user.protectedNoteIds.contains(noteId)) {
//                        val protectedNotePassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
//                        if (user.protectedNotePassword != protectedNotePassword)
//                            throw IncorrectSecuredNotesPasswordException()
//                    }
                    val result = noteResourcesDataSource.deleteResourceById(
                        noteId = noteId,
                        editorUserId = session.userId,
                        resourceId = resourceId
                    )
                    call.respond(
                        if (result) ApiResponseDto.Success(Unit)
                        else ApiResponseDto.Error()
                    )
                }

            }

            delete {
                val session = call.sessions.get<AuthSession>()!!
                val noteId = call.pathParameters[HeaderNames.NOTE_ID]!!
//                val user = usersDataSource.getUserById(session.userId)
//                if (user.protectedNoteIds.contains(noteId)) {
//                    val protectedNotePassword = call.request.headers[HeaderNames.SECURE_NOTES_PASSWORD]
//                    if (user.protectedNotePassword != protectedNotePassword)
//                        throw IncorrectSecuredNotesPasswordException()
//                }
                val resourceIdsToDelete = call.receiveNullable<List<String>>() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest)
                    return@delete
                }
                val result = noteResourcesDataSource.deleteResourceByIds(
                    noteId = noteId,
                    editorUserId = session.userId,
                    resourceIds = resourceIdsToDelete.toSet()
                )
                call.respond(
                    if (result) ApiResponseDto.Success(Unit)
                    else ApiResponseDto.Error()
                )
            }

        }

    }

}