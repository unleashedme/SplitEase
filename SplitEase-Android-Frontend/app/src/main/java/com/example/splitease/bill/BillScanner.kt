package com.example.splitease.bill

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.roundToInt

/** A bill photo made ready to upload, plus the total read from it (null if none was found). */
class ScannedBill(val jpeg: ByteArray, val detectedAmount: Double?)

interface BillScanner {
    /**
     * Shrinks the photo at [uri] for upload and, if [readTotal], reads the total off it.
     * Throws if the photo can't be opened.
     */
    suspend fun scan(uri: String, readTotal: Boolean = true): ScannedBill
}

/** Reads bills on the device with ML Kit, so the photo isn't sent anywhere to be read. */
class MlKitBillScanner(context: Context) : BillScanner {

    private val appContext = context.applicationContext
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    override suspend fun scan(uri: String, readTotal: Boolean): ScannedBill {
        val bitmap = withContext(Dispatchers.IO) { loadUpright(Uri.parse(uri)) }
        try {
            val jpeg = withContext(Dispatchers.Default) { compress(bitmap) }
            // A failed read only loses the suggestion; the photo can still be attached
            val amount = if (readTotal) runCatching { readTotalFrom(bitmap) }.getOrNull() else null
            return ScannedBill(jpeg, amount)
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun readTotalFrom(bitmap: Bitmap): Double? {
        val text = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
        val lines = text.textBlocks.flatMap { it.lines }.mapNotNull { line ->
            val box = line.boundingBox ?: return@mapNotNull null
            OcrLine(line.text, box.left, box.top, box.right, box.bottom)
        }
        return BillAmountParser.findTotal(lines)
    }

    /** Decodes at most [MAX_SIDE] pixels on the long side, turned the way the camera held it. */
    private fun loadUpright(uri: Uri): Bitmap {
        val resolver = appContext.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Not an image")

        // Decode at a power-of-two reduction first so a 12 MP photo never sits in memory at full size;
        // ending a little under MAX_SIDE is fine for reading the text
        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_SIDE * 3 / 4) sampleSize *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        } ?: throw IOException("Couldn't decode image")

        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        val scale = minOf(1f, MAX_SIDE.toFloat() / max(decoded.width, decoded.height))
        val matrix = Matrix().apply {
            postScale(scale, scale)
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            }
        }
        if (matrix.isIdentity) return decoded
        val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (upright !== decoded) decoded.recycle()
        return upright
    }

    private fun compress(bitmap: Bitmap): ByteArray {
        var quality = JPEG_QUALITY
        while (true) {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            // The backend accepts up to 5 MB; this is only reached for unusually detailed photos
            if (out.size() <= MAX_UPLOAD_BYTES || quality <= MIN_JPEG_QUALITY) return out.toByteArray()
            quality = (quality * 0.75).roundToInt()
        }
    }

    private companion object {
        // Keeps small print readable for ML Kit while staying well under the upload limit
        const val MAX_SIDE = 2048
        const val JPEG_QUALITY = 80
        const val MIN_JPEG_QUALITY = 40
        const val MAX_UPLOAD_BYTES = 4 * 1024 * 1024
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
