package com.glitch.floweryapi.domain.utils.encryptor

import io.ktor.server.config.*
import io.ktor.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val TAG = "AES ENCRYPTOR"
private const val CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding"

object AESEncryptor {

    private val secret = ApplicationConfig(null).tryGetString("app.security.encrypt_secret")
    private val iv = ApplicationConfig(null).tryGetString("app.security.encrypt_iv")

    fun encrypt(
        normalString: String,
        secretKey: String = secret!!
    ): String {
        return try {
            val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
            val ivParameterSpec = IvParameterSpec(iv!!.toByteArray())

            val plainText = normalString.toByteArray()

            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)

            val encryptedString = cipher.doFinal(plainText)
            encryptedString.encodeBase64()
        } catch (e: Exception) {
            throw EncryptionException()
        }
    }

    fun encrypt(
        normalByteArray: ByteArray,
        secretKey: String = secret!!
    ): ByteArray {
        try {
            val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
            val ivParameterSpec = IvParameterSpec(iv!!.toByteArray())

            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)

            val encryptedByteArray = cipher.doFinal(normalByteArray)
            return encryptedByteArray
        } catch (e: Exception) {
            throw EncryptionException()
        }
    }

    fun decrypt(
        encryptedString: String,
        secretKey: String = secret!!
    ): String {
        return try {
            val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
            val ivParameterSpec = IvParameterSpec(iv!!.toByteArray())

            val textToDecrypt = encryptedString.decodeBase64Bytes()

            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)

            val decryptedString = cipher.doFinal(textToDecrypt)
            decryptedString.decodeToString()
        } catch (e: Exception) {
            throw EncryptionException()
        }
    }

    fun decrypt(
        encryptedByteArray: ByteArray,
        secretKey: String = secret!!
    ): ByteArray {
        try {
            val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
            val ivParameterSpec = IvParameterSpec(iv!!.toByteArray())

            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)

            val decryptedByteArray = cipher.doFinal(encryptedByteArray)
            return decryptedByteArray
        } catch (e: Exception) {
            throw EncryptionException()
        }
    }

}