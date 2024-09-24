package com.glitch.securenotes.domain.utils.imageprocessor

import java.io.File

interface ImageProcessor {

    fun isImage(localPath: String): Boolean

    fun getImageDimensions(localPath: String): ImageDimensions

    fun compressImageWithProportions(
        inputFile: File,
        outputFile: File,
        maxDimSize: Int = ImageProcessorConstants.RECT_MAX_IMAGE_DEFAULT,
        compressionQuality: Float = ImageProcessorConstants.COMPRESSION_MODE_HIGH_QUALITY
    )

    fun compressImageAndCrop(
        inputFile: File,
        outputFile: File,
        sideSize: Int = ImageProcessorConstants.AVATAR_DEFAULT,
        compressionQuality: Float = ImageProcessorConstants.COMPRESSION_MODE_HIGH_QUALITY
    )

}