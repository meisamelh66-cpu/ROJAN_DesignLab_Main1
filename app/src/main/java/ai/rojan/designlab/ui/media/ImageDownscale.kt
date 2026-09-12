package ai.rojan.designlab.ui.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

/* =============================================================================
 * ROJAN — shared "pick an image → downscale → JPEG re-encode" pipeline.
 *
 * Promoted from `ManagerSalonMediaScreen`'s private helpers (Customer Profile
 * Personalization Phase 5B) so the Customer profile avatar/cover flow uses the
 * exact same code path the Manager salon logo/cover flow already does:
 *
 *   • Android-13+ Photo Picker ([ImageOnlyPickerRequest]) — no runtime permission.
 *   • Two-pass decode (bounds → sub-sampled full decode) keeps peak memory
 *     bounded for a large source photo.
 *   • EXIF orientation is read and baked into the pixels (re-encoding drops the
 *     orientation tag), and — since we fully re-encode to JPEG — every other
 *     EXIF field (including GPS) is stripped in the process.
 *   • Output is always a small, predictable JPEG at [JPEG_QUALITY], bounded to
 *     [maxDimension] on the longer side, so an upload never hits the request
 *     timeout on a raw 4–8 MB camera photo.
 *
 * CPU-bound — call [decodeResizeAndCompress] off the main thread
 * (`withContext(Dispatchers.Default)`).
 * ========================================================================== */

const val JPEG_QUALITY = 80

/** Longer-side pixel caps per profile-media slot (Customer Phase 5B). */
object CustomerImageBounds {
    const val AVATAR_MAX_DIMENSION = 1024
    const val COVER_MAX_DIMENSION = 1600
}

/** Android-13+ system Photo Picker request, images only — reused by every ROJAN picker. */
val ImageOnlyPickerRequest: PickVisualMediaRequest =
    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

/**
 * Downscales the picked image to fit within [maxDimension] on its longer side
 * and re-encodes it as JPEG at [quality]. Returns `(bytes, fileName, mimeType)`
 * or `null` if the URI can't be decoded. Runs no I/O or CPU work on the caller's
 * thread beyond what the caller schedules — invoke it inside a background
 * dispatcher.
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

    val outputStream = ByteArrayOutputStream()
    outputBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

    if (outputBitmap !== rotatedBitmap) rotatedBitmap.recycle()
    if (rotatedBitmap !== sampledBitmap) sampledBitmap.recycle()
    outputBitmap.recycle()

    val fileName = "rojan_image_${System.currentTimeMillis()}.jpg"
    Triple(outputStream.toByteArray(), fileName, "image/jpeg")
}.getOrNull()

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
