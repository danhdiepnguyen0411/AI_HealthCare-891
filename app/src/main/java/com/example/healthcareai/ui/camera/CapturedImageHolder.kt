package com.example.healthcareai.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import kotlin.math.max

object CapturedImageHolder {
    var bitmap: Bitmap? = null
    var imagePath: String? = null
    var imageUri: Uri? = null

    fun clear() {
        bitmap = null
        imagePath = null
        imageUri = null
    }

    /**
     * Rapidly decodes and downsamples an image to screen size with correct EXIF orientation,
     * allowing instant display (<20ms) without main-thread stalls.
     */
    fun decodeScreenScaledBitmap(filePath: String, maxDim: Int = 1600): Bitmap? {
        val cleanPath = when {
            filePath.startsWith("file://") -> filePath.removePrefix("file://")
            filePath.startsWith("file:") -> filePath.removePrefix("file:")
            else -> filePath
        }
        val file = File(cleanPath)
        if (!file.exists()) return null

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(cleanPath, options)

        val srcWidth = options.outWidth
        val srcHeight = options.outHeight
        if (srcWidth <= 0 || srcHeight <= 0) return null

        var inSampleSize = 1
        val maxSrc = max(srcWidth, srcHeight)
        while (maxSrc / (inSampleSize * 2) >= maxDim) {
            inSampleSize *= 2
        }

        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        val decoded = BitmapFactory.decodeFile(cleanPath, options) ?: return null
        return fixExifRotation(cleanPath, decoded)
    }

    fun decodeScreenScaledBitmap(context: Context, uri: Uri, maxDim: Int = 1600): Bitmap? {
        // If file scheme, decode directly via file path
        if (uri.scheme == "file" || (uri.path != null && File(uri.path ?: "").exists())) {
            val path = uri.path
            if (!path.isNullOrEmpty()) {
                val direct = decodeScreenScaledBitmap(path, maxDim)
                if (direct != null) return direct
            }
        }

        return try {
            // Read EXIF orientation before or during stream read
            val orientation = try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (_: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }

            val stream1 = context.contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream1, null, options)
            stream1.close()

            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return null

            var inSampleSize = 1
            val maxSrc = max(srcWidth, srcHeight)
            while (maxSrc / (inSampleSize * 2) >= maxDim) {
                inSampleSize *= 2
            }

            val stream2 = context.contentResolver.openInputStream(uri) ?: return null
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            val decoded = BitmapFactory.decodeStream(stream2, null, options)
            stream2.close()

            if (decoded != null && orientation != ExifInterface.ORIENTATION_NORMAL) {
                val rotationAngle = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
                if (rotationAngle != 0f) {
                    val matrix = Matrix().apply { postRotate(rotationAngle) }
                    Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                } else {
                    decoded
                }
            } else {
                decoded
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun fixExifRotation(filePath: String, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val rotationAngle = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (rotationAngle != 0f) {
                val matrix = Matrix().apply { postRotate(rotationAngle) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (_: Exception) {
            bitmap
        }
    }
}
