package com.glitch.securenotes.domain.utils.filemanager

import java.io.File

interface FileManager {

    fun getFile(localPath: String): File

    fun getFileBytes(localPath: String): ByteArray

    fun uploadFile(
        fileName: String,
        fileBytes: ByteArray,
        fileNameSuffix: String = ""
    ): String

    fun updateFileContent(
        localPath: String,
        newByteArray: ByteArray
    )

    fun deleteFile(localPath: String): Boolean

    fun toLocalPath(urlPath: String): String

    fun toUrlPath(localPath: String): String

}