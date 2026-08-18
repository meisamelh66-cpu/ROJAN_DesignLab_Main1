package ai.rojan.designlab.manager.screens.settings

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.domain.media.ManagerMediaAsset
import ai.rojan.designlab.manager.presentation.settings.ManagerSalonMediaViewModel
import ai.rojan.designlab.manager.presentation.settings.SalonMediaState
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.ui.components.image.RojanRemoteImage
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Central Salon Management — Salon Media UI. A management layer over the
 * existing Media Foundation, reached from
 * [ManagerSalonSetupScreen]'s "لوگو و تصویر کاور" entry point (previously
 * a static, honestly-labeled placeholder — the backend upload
 * infrastructure it was waiting on has existed since Phase 1). Every
 * asset here is stored through the real, unmodified
 * `POST/GET/DELETE /api/v1/salons/{salonId}/media` and
 * `PUT /api/v1/salons/{salonId}/identity-media` contracts — no
 * Manager-only media structure; a future Customer App/Reception App/
 * Website reads the identical rows this screen writes.
 */
@Composable
fun ManagerSalonMediaScreen(
    viewModel: ManagerSalonMediaViewModel,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
) {
    val loadState by viewModel.loadState.collectAsState()

    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        when (val state = loadState) {
            is UiState.Loading, is UiState.Empty -> SalonMediaLoading()
            is UiState.Error -> SalonMediaLoadError(message = state.message, onRetry = viewModel::load)
            is UiState.Success -> SalonMediaContent(
                state = state.data,
                onLogoPicked = viewModel::uploadLogo,
                onCoverPicked = viewModel::uploadCover,
                onGalleryImagePicked = viewModel::addGalleryImage,
                onGalleryImageDelete = viewModel::deleteGalleryImage,
            )
        }
    }
}

@Composable
private fun SalonMediaLoading() {
    Column(
        modifier = Modifier.fillMaxSize().padding(RojanDimens.SpaceXXL),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = ManagerColors.Turquoise)
    }
}

@Composable
private fun SalonMediaLoadError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, style = RojanTypography.Body, color = RojanErrorText)
        ManagerPrimaryButton(text = "تلاش مجدد", onClick = onRetry)
    }
}

@Composable
private fun SalonMediaContent(
    state: SalonMediaState,
    onLogoPicked: (ByteArray, String, String) -> Unit,
    onCoverPicked: (ByteArray, String, String) -> Unit,
    onGalleryImagePicked: (ByteArray, String, String) -> Unit,
    onGalleryImageDelete: (String) -> Unit,
) {
    val context = LocalContext.current
    // ANR fix: decoding/resizing/compressing reads the full file and runs
    // CPU-bound bitmap work - previously done directly inside the
    // ActivityResultCallback, which always runs on the main thread. Kept
    // off the main thread here via Dispatchers.Default (CPU-bound, not
    // I/O-bound - decode+compress dominate the work).
    //
    // Upload-timeout fix: raw picker output for a modern phone camera
    // photo is routinely 4-8MB, slow enough over a mobile upload to hit
    // the client request timeout. decodeResizeAndCompress() downscales to
    // a per-slot max dimension and re-encodes as JPEG at ~80% quality
    // before upload, so every asset this screen sends is a small,
    // standard, predictable size - not a backend/timeout-config change.
    val coroutineScope = rememberCoroutineScope()

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { picked ->
            coroutineScope.launch {
                withContext(Dispatchers.Default) { decodeResizeAndCompress(picked, context, maxDimension = LOGO_MAX_DIMENSION) }
                    ?.let { (bytes, name, mime) -> onLogoPicked(bytes, name, mime) }
            }
        }
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { picked ->
            coroutineScope.launch {
                withContext(Dispatchers.Default) { decodeResizeAndCompress(picked, context, maxDimension = COVER_MAX_DIMENSION) }
                    ?.let { (bytes, name, mime) -> onCoverPicked(bytes, name, mime) }
            }
        }
    }
    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { picked ->
            coroutineScope.launch {
                withContext(Dispatchers.Default) { decodeResizeAndCompress(picked, context, maxDimension = GALLERY_MAX_DIMENSION) }
                    ?.let { (bytes, name, mime) -> onGalleryImagePicked(bytes, name, mime) }
            }
        }
    }
    val imageOnlyRequest = remember { PickVisualMediaImageOnlyRequest }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(RojanDimens.SpaceMD),
        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        Text(text = "رسانه سالن", style = RojanTypography.ScreenTitle, color = ManagerColors.TextPrimary)

        if (state.errorMessage != null) {
            Text(text = state.errorMessage, style = RojanTypography.Caption, color = RojanErrorText)
        }

        IdentityMediaCard(
            title = "لوگو",
            url = state.logoUrl,
            placeholderIcon = Icons.Filled.Store,
            aspectRatio = 1f,
            isUploading = state.isUploadingLogo,
            onChangeClick = { logoPicker.launch(imageOnlyRequest) },
        )

        IdentityMediaCard(
            title = "تصویر کاور",
            url = state.coverUrl,
            placeholderIcon = Icons.Filled.Image,
            aspectRatio = 16f / 9f,
            isUploading = state.isUploadingCover,
            onChangeClick = { coverPicker.launch(imageOnlyRequest) },
        )

        GallerySection(
            images = state.gallery,
            uploadingCount = state.uploadingGalleryCount,
            onAddClick = { galleryPicker.launch(imageOnlyRequest) },
            onDeleteClick = onGalleryImageDelete,
        )
    }
}

@Composable
private fun IdentityMediaCard(
    title: String,
    url: String?,
    placeholderIcon: ImageVector,
    aspectRatio: Float,
    isUploading: Boolean,
    onChangeClick: () -> Unit,
) {
    ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            Text(text = title, style = RojanTypography.CardTitle, color = ManagerColors.TextPrimary)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .background(ManagerColors.BaseSecondary, RojanShapes.Small),
                contentAlignment = Alignment.Center,
            ) {
                RojanRemoteImage(
                    url = url,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    shape = RojanShapes.Small,
                    fallback = { Icon(imageVector = placeholderIcon, contentDescription = null, tint = ManagerColors.TextSecondary) },
                )
                if (isUploading) {
                    Box(modifier = Modifier.fillMaxSize().background(ManagerColors.BaseDeep.copy(alpha = 0.55f), RojanShapes.Small), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ManagerColors.Turquoise)
                    }
                }
            }

            ManagerPrimaryButton(
                text = if (isUploading) "در حال آپلود..." else if (url == null) "افزودن $title" else "تغییر $title",
                enabled = !isUploading,
                onClick = onChangeClick,
            )
        }
    }
}

@Composable
private fun GallerySection(
    images: List<ManagerMediaAsset>,
    uploadingCount: Int,
    onAddClick: () -> Unit,
    onDeleteClick: (String) -> Unit,
) {
    ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "گالری تصاویر", style = RojanTypography.CardTitle, color = ManagerColors.TextPrimary)
                Text(text = "${images.size} تصویر", style = RojanTypography.Caption, color = ManagerColors.TextSecondary)
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth().height(gridHeight(images.size + 1)),
                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
            ) {
                items(images, key = { it.id }) { image ->
                    GalleryThumbnail(url = image.url, onDeleteClick = { onDeleteClick(image.id) })
                }
                items(uploadingCount) {
                    Box(
                        modifier = Modifier.aspectRatio(1f).background(ManagerColors.BaseSecondary, RojanShapes.Small),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = ManagerColors.Turquoise)
                    }
                }
                item {
                    AddGalleryTile(onClick = onAddClick)
                }
            }
        }
    }
}

@Composable
private fun GalleryThumbnail(url: String, onDeleteClick: () -> Unit) {
    Box(modifier = Modifier.aspectRatio(1f)) {
        RojanRemoteImage(
            url = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            shape = RojanShapes.Small,
            fallback = { Icon(imageVector = Icons.Filled.Image, contentDescription = null, tint = ManagerColors.TextSecondary) },
        )
        Box(
            modifier = Modifier
                .padding(RojanDimens.SpaceXS)
                .align(Alignment.TopEnd)
                .size(24.dp)
                .background(ManagerColors.BaseDeep.copy(alpha = 0.7f), CircleShape)
                .rojanPressable(onClick = onDeleteClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = "حذف تصویر", tint = ManagerColors.TextPrimary, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun AddGalleryTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(ManagerColors.BaseSecondary, RojanShapes.Small)
            .rojanPressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = "افزودن تصویر", tint = ManagerColors.Turquoise)
    }
}

/** Three columns, each cell square (`aspectRatio(1f)`) — approximates the grid's total height without a full custom layout, same pragmatic shape [WorkingHoursForm] and other fixed-height Manager lists use for `LazyVerticalGrid` inside a non-scrolling parent `Column`. */
private fun gridHeight(itemCount: Int): androidx.compose.ui.unit.Dp {
    val rows = (itemCount + 2) / 3
    return (rows * 120).dp
}

private val PickVisualMediaImageOnlyRequest =
    androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

private const val LOGO_MAX_DIMENSION = 1024
private const val COVER_MAX_DIMENSION = 1600
private const val GALLERY_MAX_DIMENSION = 2048
private const val JPEG_QUALITY = 80

/**
 * Downscales the picked image to fit within [maxDimension] on its longer
 * side and re-encodes it as JPEG at [JPEG_QUALITY] - standard output for
 * every upload slot, regardless of the source file's format/size. Two-pass
 * decode (bounds first, then a sub-sampled full decode) keeps peak memory
 * bounded for large source photos; EXIF orientation is read and baked into
 * the pixel data since re-encoding drops the original orientation tag.
 */
private fun decodeResizeAndCompress(
    uri: Uri,
    context: android.content.Context,
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

    val fileName = "salon_media_${System.currentTimeMillis()}.jpg"
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
