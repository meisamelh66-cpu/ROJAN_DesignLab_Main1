package ai.rojan.designlab.ui.components.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri

/**
 * Shared upload-prep pipeline for every Manager media picker (Central Salon
 * Management — Salon Media UI, and Media Sprint P0's specialist avatar
 * flow) — extracted from `ManagerSalonMediaScreen`'s original logo/cover/
 * gallery-only implementation so a second upload surface (specialist
 * avatar) doesn't duplicate it.
 *
 * ANR fix: decoding/resizing/compressing reads the full file and runs
 * CPU-bound bitmap work — callers must invoke this off the main thread
 * (`Dispatchers.Default`; CPU-bound, not I/O-bound — decode+compress
 * dominate the work).
 *
 * Upload-timeout fix: raw picker output for a modern phone camera photo is
 * routinely 4-8MB, slow enough over a mobile upload to hit the client
 * request timeout. Downscales to [maxDimension] on the longer side and
 * re-encodes as JPEG at [quality] before upload, so every asset any picker
 * sends is a small, standard, predictable size — not a backend/timeout-
 * config change.
 *
 * Two-pass decode (bounds first, then a sub-sampled full decode) keeps peak
 * memory bounded for large source photos; EXIF orientation is read and
 * baked into the pixel data since re-encoding drops the original
 * orientation tag.
 */
fun decodeResizeAndCompress(
    uri: Uri,
    context: Context,
    maxDimension: Int,
    quality: Int = JPEG_QUALITY,
): Triple<ByteArray, String, String>? = runCatching {
    val resolver = context.contentResolver

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val boundsStream = resolver.openInputStream(uri) ?: return null
    boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
    }
    val sampledBitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) } ?: return null

    val orientation = resolver.openInputStream(uri)?.use {
        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL

    val rotatedBitmap = applyExifRotation(sampledBitmap, orientation)
    val outputBitmap = scaleToMaxDimension(rotatedBitmap, maxDimension)

    val outputStream = java.io.ByteArrayOutputStream()
    outputBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

    if (outputBitmap !== rotatedBitmap) rotatedBitmap.recycle()
    if (rotatedBitmap !== sampledBitmap) sampledBitmap.recycle()
    outputBitmap.recycle()

    val fileName = "upload_${System.currentTimeMillis()}.jpg"
    Triple(outputStream.toByteArray(), fileName, "image/jpeg")
}.getOrNull()

const val JPEG_QUALITY = 80

private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    var inSampleSize = 1
    val longerSide = maxOf(width, height)
    while (longerSide / (inSampleSize * 2) >= maxDimension) {
        inSampleSize *= 2
    }
    return inSampleSize
}

private fun applyExifRotation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun scaleToMaxDimension(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val longerSide = maxOf(bitmap.width, bitmap.height)
    if (longerSide <= maxDimension) return bitmap
    val scale = maxDimension.toFloat() / longerSide
    val targetWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
}
