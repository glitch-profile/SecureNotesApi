package com.glitch.securenotes.domain.utils.filemanager

import io.ktor.server.config.*
import java.io.File
import java.nio.file.Paths
import java.util.*

class FileManagerImpl: FileManager {

    private val isPackedForExternal = ApplicationConfig(null).tryGetString("app.is_for_external").toBoolean()

    override fun getFile(localPath: String): File {
        val file = File(localPath)
        if (file.exists() && file.canRead()) return file
        else throw FileAccessException()
    }

    override fun getFileBytes(localPath: String): ByteArray {
        val file = File(localPath)
        if (file.exists() && file.canRead()) {
            return file.inputStream().use {
                it.readBytes()
            }
        } else throw FileAccessException()
    }

    override fun uploadFile(fileName: String, fileBytes: ByteArray, fileNameSuffix: String): String {
        val fileData = File(fileName)
        if (fileData.extension.isEmpty()) throw UnknownExtensionException()
        val filePath = "${getResourcePath()}/${generateDirectoryPath()}$fileNameSuffix.${fileData.extension}"
        File(filePath).outputStream().use {
            it.write(fileBytes)
        }
        return filePath
    }

    override fun updateFileContent(localPath: String, newByteArray: ByteArray) {
        val file = getFile(localPath)
        file.outputStream().use {
            it.write(newByteArray)
        }
    }

    override fun deleteFile(localPath: String): Boolean {
        val file = getFile(localPath)
        return file.delete()
    }

    override fun toLocalPath(urlPath: String): String {
        return "${getResourcePath()}/$urlPath"
    }

    override fun toUrlPath(localPath: String): String {
        val resourcePath = getResourcePath()
        return localPath.replace(
            oldValue = resourcePath,
            newValue = ""
        )
    }

    private fun getResourcePath() = if (isPackedForExternal) {
        "${Paths.get("")}/resources"
    } else {
        "build/.resources"
    }

    private fun generateDirectoryPath(): String {
        val randomString = UUID.randomUUID().toString().filterNot { it == '-' }
        val directoryTree = randomString.chunked(2)
            .joinToString("/")
        return directoryTree
    }
}