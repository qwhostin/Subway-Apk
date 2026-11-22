package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Local-only face cropping and 3D texture preparation for RUNVANTA's "Play As Anyone" feature.
 * Never uploads photographs to any server and deletes any temporary image files after processing.
 */
object PhotoFaceProcessor {

    private const val FACES_DIR = "faces"
    private const val OUTPUT_SIZE = 256

    suspend fun loadPreviewBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        try {
            tempFile = File.createTempFile("runvanta_pick_", ".tmp", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(tempFile.absolutePath, bounds)
            val maxDim = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
            val sampleSize = calculateSampleSize(maxDim, 768)

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = BitmapFactory.decodeFile(tempFile.absolutePath, decodeOpts)
                ?: return@withContext null

            val rotation = readExifRotation(tempFile)
            if (rotation == 0f) {
                decoded
            } else {
                val matrix = Matrix().apply { postRotate(rotation) }
                val rotated = Bitmap.createBitmap(
                    decoded, 0, 0, decoded.width, decoded.height, matrix, true
                )
                if (rotated !== decoded) decoded.recycle()
                rotated
            }
        } catch (_: Exception) {
            null
        } finally {
            tempFile?.delete()
        }
    }

    /**
     * Bakes a cropped, scaled, offset 256x256 face texture PNG with a smooth cartoon-blended
     * skin border so it maps seamlessly onto the 3D character head front plate.
     */
    suspend fun bakeFaceTexture(
        context: Context,
        sourceBitmap: Bitmap,
        faceScale: Float,
        faceOffsetX: Float,
        faceOffsetY: Float,
        skinColorRgb: Long,
        existingPathToReplace: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val facesDir = File(context.filesDir, FACES_DIR).apply { mkdirs() }
            val outFile = File(facesDir, "face_${UUID.randomUUID()}.png")

            val outBitmap = Bitmap.createBitmap(OUTPUT_SIZE, OUTPUT_SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(outBitmap)

            // Base skin-tone fill around the face oval so edges blend smoothly onto the 3D head
            val skinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = (0xFF000000L or (skinColorRgb and 0xFFFFFFL)).toInt()
            }
            canvas.drawRect(0f, 0f, OUTPUT_SIZE.toFloat(), OUTPUT_SIZE.toFloat(), skinPaint)

            // Clip to soft oval face mask inside the texture
            val ovalPath = Path().apply {
                addOval(
                    RectF(
                        OUTPUT_SIZE * 0.08f,
                        OUTPUT_SIZE * 0.06f,
                        OUTPUT_SIZE * 0.92f,
                        OUTPUT_SIZE * 0.94f
                    ),
                    Path.Direction.CW
                )
            }
            canvas.save()
            canvas.clipPath(ovalPath)

            val srcW = sourceBitmap.width.toFloat().coerceAtLeast(1f)
            val srcH = sourceBitmap.height.toFloat().coerceAtLeast(1f)
            val baseScale = maxOf(OUTPUT_SIZE / srcW, OUTPUT_SIZE / srcH)
            val finalScale = baseScale * faceScale.coerceIn(0.6f, 2.8f)

            val drawW = srcW * finalScale
            val drawH = srcH * finalScale
            val left = (OUTPUT_SIZE - drawW) / 2f + (faceOffsetX * OUTPUT_SIZE * 0.35f)
            val top = (OUTPUT_SIZE - drawH) / 2f + (faceOffsetY * OUTPUT_SIZE * 0.35f)

            val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(
                sourceBitmap,
                null,
                RectF(left, top, left + drawW, top + drawH),
                bitmapPaint
            )
            canvas.restore()

            FileOutputStream(outFile).use { fos ->
                outBitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
            }
            outBitmap.recycle()

            if (!existingPathToReplace.isNullOrBlank() && existingPathToReplace != outFile.absolutePath) {
                deleteFaceFile(existingPathToReplace)
            }
            // Also purge any stray temp files in cacheDir
            cleanupTempFiles(context)

            outFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun deleteFaceFile(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching {
            val file = File(path)
            if (file.exists()) file.delete()
        }
    }

    fun deleteAllCustomFaces(context: Context) {
        runCatching {
            val facesDir = File(context.filesDir, FACES_DIR)
            if (facesDir.exists()) {
                facesDir.listFiles()?.forEach { it.delete() }
            }
            cleanupTempFiles(context)
        }
    }

    private fun cleanupTempFiles(context: Context) {
        runCatching {
            context.cacheDir.listFiles()?.forEach { f ->
                if (f.name.startsWith("runvanta_pick_")) {
                    f.delete()
                }
            }
        }
    }

    private fun calculateSampleSize(maxDim: Int, targetMax: Int): Int {
        var sample = 1
        while (maxDim / sample > targetMax * 2) {
            sample *= 2
        }
        return sample
    }

    private fun readExifRotation(file: File): Float {
        return runCatching {
            val exif = ExifInterface(file.absolutePath)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)
    }
}
