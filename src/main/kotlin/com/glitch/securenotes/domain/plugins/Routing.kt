package com.glitch.securenotes.domain.plugins

import com.glitch.floweryapi.domain.utils.encryptor.AESEncryptor
import com.glitch.securenotes.data.datasource.AuthSessionStorage
import com.glitch.securenotes.data.datasource.UserCollectionsDataSource
import com.glitch.securenotes.data.datasource.UserCredentialsDataSource
import com.glitch.securenotes.data.datasource.UsersDataSource
import com.glitch.securenotes.data.datasource.notes.NoteResourcesDataSource
import com.glitch.securenotes.data.datasource.notes.NotesDataSource
import com.glitch.securenotes.domain.rooms.noteslist.UserNotesRoomController
import com.glitch.securenotes.domain.routes.*
import com.glitch.securenotes.domain.utils.codeauth.CodeAuthenticator
import com.glitch.securenotes.domain.utils.filemanager.FileManager
import com.glitch.securenotes.domain.utils.imageprocessor.ImageProcessor
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.io.File

fun Application.configureRouting() {
    val codeAuthenticator by inject<CodeAuthenticator>()
    val authSessionManager by inject<AuthSessionStorage>()

    val userCredentialsDataSource by inject<UserCredentialsDataSource>()
    val usersDataSource by inject<UsersDataSource>()

    val fileManager by inject<FileManager>()
    val imageProcessor by inject<ImageProcessor>()

    val userCollectionsDataSource by inject<UserCollectionsDataSource>()
    val notesDataSource by inject<NotesDataSource>()
    val noteResourcesDataSource by inject<NoteResourcesDataSource>()
    val notesRoomController by inject<UserNotesRoomController>()

    routing {

        authRoutes(
            userCredentialsDataSource,
            usersDataSource,
            codeAuthenticator,
            authSessionManager
        )
        userRoutes(
            usersDataSource = usersDataSource,
            userCredentialsDataSource = userCredentialsDataSource,
            authSessionStorage = authSessionManager,
            fileManager = fileManager,
            notesDataSource = notesDataSource,
            noteResourcesDataSource = noteResourcesDataSource,
            imageProcessor = imageProcessor
        )
        noteRoutes(
            usersDataSource = usersDataSource,
            notesDataSource = notesDataSource,
            noteResourcesDataSource = noteResourcesDataSource,
            notesRoomController = notesRoomController
        )
        resourceRoutes(
            usersDataSource = usersDataSource,
            notesDataSource = notesDataSource,
            noteResourcesDataSource = noteResourcesDataSource,
            fileManager = fileManager,
            imageProcessor = imageProcessor
        )
        utilRoutes()

        // should use this instead of static resources
        get("test") {
            val file = File("D:/test/123.png")
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.FileName, "test.jpg").toString()
            )
            val byteArray = file.inputStream().use {
                it.readBytes()
            }
            call.respondBytes(byteArray)
        }
        get("test2") {
            val hasAuthority = call.request.header("user_id") == "12345"
            if (!hasAuthority) call.respond(HttpStatusCode.Forbidden)
            val file = File("D:/test/123.jpg")
            val byteArray = file.inputStream().use {
                it.readBytes()
            }
            call.response.header(
                HttpHeaders.ContentType,
                ContentType.defaultForFile(file).toString()
            )
            call.respondBytes(byteArray)
        }
        get("test3") {
            val file = File("F:/test/test-e.txt")
            val byteArray = file.inputStream().use {
                it.readBytes()
            }
            val decryptedBytes = AESEncryptor.decrypt(byteArray)
            call.response.header(
                HttpHeaders.ContentType,
                ContentType.defaultForFile(file).toString()
            )
            call.respondBytes(decryptedBytes)
        }
        post("test4") {
            val list = listOf(1, 2, 3, 4, 5)
            val list2= listOf(1, 2, 3, 4, 5).toMutableList().apply {
                removeFirst()
            }
            println(list)
            println(list2)
        }
    }
}
