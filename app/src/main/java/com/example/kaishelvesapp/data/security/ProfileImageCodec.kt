package com.example.kaishelvesapp.data.security

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object ProfileImageCodec {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "kai_shelves_profile_image_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val MAX_IMAGE_SIZE = 256
    private const val JPEG_QUALITY = 72
    private const val DATA_URI_PREFIX = "data:image/jpeg;base64,"

    fun encodeEncryptedImage(context: Context, uri: Uri): String {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: error("No se pudo leer la imagen seleccionada")

        val compressedBytes = compressForProfile(rawBytes)
        val base64Image = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
        return encryptText(base64Image)
    }

    fun encodeImageAsDataUri(context: Context, uri: Uri): String {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: error("No se pudo leer la imagen seleccionada")

        val compressedBytes = compressForProfile(rawBytes)
        val base64Image = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
        return DATA_URI_PREFIX + base64Image
    }

    fun cropImageAsDataUri(
        context: Context,
        uri: Uri,
        viewportSizePx: Int,
        zoom: Float,
        offsetX: Float,
        offsetY: Float
    ): String {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: error("No se pudo leer la imagen seleccionada")

        val sourceBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            ?: error("No se pudo preparar la imagen seleccionada")

        val baseScale = maxOf(
            viewportSizePx.toFloat() / sourceBitmap.width.toFloat(),
            viewportSizePx.toFloat() / sourceBitmap.height.toFloat()
        )
        val totalScale = baseScale * zoom.coerceAtLeast(1f)
        val scaledWidth = sourceBitmap.width * totalScale
        val scaledHeight = sourceBitmap.height * totalScale
        val imageLeft = viewportSizePx / 2f + offsetX - scaledWidth / 2f
        val imageTop = viewportSizePx / 2f + offsetY - scaledHeight / 2f

        val cropLeft = ((-imageLeft) / totalScale).toInt()
            .coerceIn(0, sourceBitmap.width - 1)
        val cropTop = ((-imageTop) / totalScale).toInt()
            .coerceIn(0, sourceBitmap.height - 1)
        val cropSize = (viewportSizePx / totalScale).toInt()
            .coerceAtLeast(1)
        val cropWidth = cropSize.coerceAtMost(sourceBitmap.width - cropLeft)
        val cropHeight = cropSize.coerceAtMost(sourceBitmap.height - cropTop)

        val croppedBitmap = Bitmap.createBitmap(
            sourceBitmap,
            cropLeft,
            cropTop,
            cropWidth,
            cropHeight
        )
        val compressedBytes = compressBitmapForProfile(croppedBitmap)
        val base64Image = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
        return DATA_URI_PREFIX + base64Image
    }

    fun decryptImageBytes(encryptedPayload: String): ByteArray? {
        return runCatching {
            val base64Image = decryptText(encryptedPayload)
            Base64.decode(base64Image, Base64.DEFAULT)
        }.getOrNull()
    }

    private fun compressForProfile(rawBytes: ByteArray): ByteArray {
        val originalBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            ?: return rawBytes

        val scaledBitmap = Bitmap.createScaledBitmap(
            originalBitmap,
            MAX_IMAGE_SIZE,
            MAX_IMAGE_SIZE,
            true
        )

        return ByteArrayOutputStream().use { output ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            output.toByteArray()
        }
    }

    private fun compressBitmapForProfile(bitmap: Bitmap): ByteArray {
        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            MAX_IMAGE_SIZE,
            MAX_IMAGE_SIZE,
            true
        )

        return ByteArrayOutputStream().use { output ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            output.toByteArray()
        }
    }

    private fun encryptText(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val payload = cipher.iv + encryptedBytes
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decryptText(encryptedPayload: String): String {
        val payload = Base64.decode(encryptedPayload, Base64.DEFAULT)
        val iv = payload.copyOfRange(0, IV_SIZE_BYTES)
        val encryptedBytes = payload.copyOfRange(IV_SIZE_BYTES, payload.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        )

        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
