package com.glitch.securenotes.domain.utils.filemanager

import io.ktor.server.config.*
import java.io.File
import java.nio.file.Paths
import java.util.*

class FileManagerImpl: FileManager {

    private val isPackedForExternal = ApplicationConfig(null).tryGetString("app.is_for_external").toBoolean()

    override fun generateFilePath(fileExtension: String, fileNameSuffix: String): String {
        return if (fileExtension.isBlank()) throw UnknownExtensionException()
        else {
            val isExtensionWithDot = fileExtension.startsWith('.')
            "${getResourcePath()}/${generateDirectoryPath()}$fileNameSuffix" + if (isExtensionWithDot)
                fileExtension else ".$fileExtension"
        }
    }

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

    override fun uploadFile(fileBytes: ByteArray, fileExtension: String, fileNameSuffix: String): String {
        if (fileExtension.isBlank()) throw UnknownExtensionException()
        val file = File(generateFilePath(fileExtension = fileExtension, fileNameSuffix = fileNameSuffix))
        file.parentFile.mkdirs()
        file.outputStream().use {
            it.write(fileBytes)
        }
        return file.path
    }

    override fun uploadTempFile(fileBytes: ByteArray, fileExtension: String): String {
        if (fileExtension.isBlank()) throw UnknownExtensionException()
        val tempFileName = UUID.randomUUID().toString().filterNot { it == '-' }
        val tempFilePath = "${getResourcePath()}/.temp/$tempFileName.$fileExtension"
        val file = File(tempFilePath)
        file.parentFile.mkdirs()
        file.outputStream().use { it.write(fileBytes) }
        return file.path
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
        val extension = File(urlPath).extension
        if (extension.isEmpty()) {
            val longUrlPath = urlPath
                .chunkedSequence(2)
                .joinToString("/")
            return "${getResourcePath()}/$longUrlPath"
        } else {
            val longUrlPath = urlPath.dropLast(extension.length + 1)
                .chunkedSequence(2)
                .joinToString("/")
            return "${getResourcePath()}/$longUrlPath.$extension"
        }
    }

    override fun toUrlPath(localPath: String): String {
        val resourcePath = getResourcePath()
        val urlLongPath =  localPath.replace(
            oldValue = resourcePath,
            newValue = ""
        )
        val extension = File(urlLongPath).extension
        if (extension.isEmpty()) {
            return urlLongPath.filterNot { it == '/' }
        } else {
            val urlCompact =  urlLongPath.dropLast(extension.length + 1) // including the dot
                .filterNot { it == '/' }
            return "$urlCompact.$extension"
        }
    }

    private fun getResourcePath() = if (isPackedForExternal) {
        "${Paths.get("")}/resources"
    } else {
        "build/.resources"
    }

    private fun generateDirectoryPath(): String {
        val randomString = UUID.randomUUID().toString().filterNot { it == '-' }
        val directoryTree = randomString.chunkedSequence(2)
            .joinToString("/")
        return directoryTree
    }
}