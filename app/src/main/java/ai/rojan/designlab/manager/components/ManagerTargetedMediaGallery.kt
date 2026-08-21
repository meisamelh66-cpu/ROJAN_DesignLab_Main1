package ai.rojan.designlab.manager.components

import ai.rojan.designlab.manager.domain.media.ManagerMediaAsset
import ai.rojan.designlab.manager.domain.media.ManagerMediaType
import ai.rojan.designlab.manager.domain.repository.ManagerMediaRepository
import ai.rojan.designlab.presentation.common.userMessageFor
import ai.rojan.designlab.ui.components.image.MediaPreviewDialog
import ai.rojan.designlab.ui.components.image.RojanRemoteImage
import ai.rojan.designlab.ui.components.image.decodeResizeAndCompress
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Complete Salon Gallery UX / Specialist Portfolio / Service Images (Media
 * System Evolution v2): one reusable, self-contained "media collection"
 * manager - upload, preview (via [MediaPreviewDialog]), reorder (up/down; a
 * full drag-and-drop reorder is a disclosed simplification left for later -
 * up/down buttons call the exact same `reorder` endpoint a drag gesture
 * would, just without the gesture), delete, and honest empty/loading
 * states. One component for all three "many images" media collections this
 * app has: [ManagerMediaType.GALLERY] ([targetId] `null` - salon-flat,
 * [ai.rojan.designlab.manager.screens.settings.ManagerSalonMediaScreen]),
 * [ManagerMediaType.PORTFOLIO] (specialist edit screen), and
 * [ManagerMediaType.SERVICE_IMAGE] (service edit screen) - identical UX,
 * only [mediaType]/[targetId]/[title] differ per caller. Salon identity
 * slots (logo/cover) are a different concern (single image, "assign"
 * semantics) and stay on their own `IdentityMediaCard`, not this component.
 *
 * No ViewModel - mirrors [ai.rojan.designlab.manager.screens.staff.ManagerStaffEditScreen]'s
 * own "direct repository + local state" shape, since this is a
 * self-contained section embedded inside a larger screen, not a standalone
 * screen of its own.
 */
@Composable
fun ManagerTargetedMediaGallery(
    title: String,
    salonId: String,
    mediaType: ManagerMediaType,
    targetId: String?,
    repository: ManagerMediaRepository,
    modifier: Modifier = Modifier,
    maxDimension: Int = 1600,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var images by remember(targetId) { mutableStateOf<List<ManagerMediaAsset>?>(null) }
    var errorMessage by remember(targetId) { mutableStateOf<String?>(null) }
    var uploadingCount by remember(targetId) { mutableIntStateOf(0) }
    var previewIndex by remember { mutableStateOf<Int?>(null) }

    suspend fun load() {
        repository.list(salonId, mediaType, targetId)
            .onSuccess { images = it }
            .onFailure { errorMessage = userMessageFor(it) }
    }

    LaunchedEffect(targetId) { load() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        errorMessage = null
        uploadingCount += 1
        scope.launch {
            val compressed = withContext(Dispatchers.Default) { decodeResizeAndCompress(uri, context, maxDimension = maxDimension) }
            if (compressed == null) {
                uploadingCount -= 1
                errorMessage = "بارگذاری تصویر ناموفق بود"
                return@launch
            }
            val (bytes, fileName, mimeType) = compressed
            repository.upload(salonId, mediaType, bytes, fileName, mimeType, targetId)
                .onSuccess { asset -> images = (images.orEmpty()) + asset }
                .onFailure { errorMessage = userMessageFor(it) }
            uploadingCount -= 1
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
        Text(text = title, style = RojanTypography.CardTitle, color = ManagerColors.TextPrimary)

        if (errorMessage != null) {
            Text(text = errorMessage.orEmpty(), style = RojanTypography.Caption, color = RojanErrorText)
        }

        val current = images
        when {
            current == null -> Box(
                modifier = Modifier.fillMaxWidth().height(96.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = ManagerColors.Turquoise) }

            current.isEmpty() && uploadingCount == 0 -> EmptyTargetedMediaState(
                title = title,
                onAddClick = { picker.launch(PickVisualMediaImageOnlyRequest) },
            )

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth().height(gridHeightFor(current.size + uploadingCount + 1)),
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                ) {
                    items(current, key = { it.id }) { asset ->
                        val index = current.indexOf(asset)
                        TargetedMediaThumbnail(
                            url = asset.url,
                            canMoveBack = index > 0,
                            canMoveForward = index < current.lastIndex,
                            onClick = { previewIndex = index },
                            onMoveBack = {
                                val reordered = current.toMutableList().apply { add(index - 1, removeAt(index)) }
                                images = reordered
                                scope.launch {
                                    repository.reorder(salonId, mediaType, targetId, reordered.map { it.id })
                                        .onFailure { errorMessage = userMessageFor(it); load() }
                                }
                            },
                            onMoveForward = {
                                val reordered = current.toMutableList().apply { add(index + 1, removeAt(index)) }
                                images = reordered
                                scope.launch {
                                    repository.reorder(salonId, mediaType, targetId, reordered.map { it.id })
                                        .onFailure { errorMessage = userMessageFor(it); load() }
                                }
                            },
                            onDeleteClick = {
                                val previous = current
                                images = current.filterNot { it.id == asset.id }
                                scope.launch {
                                    repository.delete(salonId, asset.id)
                                        .onFailure { errorMessage = userMessageFor(it); images = previous }
                                }
                            },
                        )
                    }
                    items(uploadingCount) {
                        Box(
                            modifier = Modifier.aspectRatio(1f).background(ManagerColors.BaseSecondary, RojanShapes.Small),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator(color = ManagerColors.Turquoise) }
                    }
                    item { AddTargetedMediaTile(onClick = { picker.launch(PickVisualMediaImageOnlyRequest) }) }
                }
            }
        }
    }

    previewIndex?.let { index ->
        MediaPreviewDialog(urls = images.orEmpty().map { it.url }, initialIndex = index, onDismiss = { previewIndex = null })
    }
}

@Composable
private fun EmptyTargetedMediaState(title: String, onAddClick: () -> Unit) {
    ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceLG),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            Icon(imageVector = Icons.Filled.Image, contentDescription = null, tint = ManagerColors.TextSecondary)
            Text(text = "هنوز تصویری اضافه نشده است", style = RojanTypography.Caption, color = ManagerColors.TextSecondary)
            ManagerPrimaryButton(text = "افزودن تصویر", onClick = onAddClick)
        }
    }
}

@Composable
private fun TargetedMediaThumbnail(
    url: String,
    canMoveBack: Boolean,
    canMoveForward: Boolean,
    onClick: () -> Unit,
    onMoveBack: () -> Unit,
    onMoveForward: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Box(modifier = Modifier.aspectRatio(1f)) {
        RojanRemoteImage(
            url = url,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).rojanPressable(onClick = onClick),
            shape = RojanShapes.Small,
            fallback = { Icon(imageVector = Icons.Filled.Image, contentDescription = null, tint = ManagerColors.TextSecondary) },
        )
        Box(
            modifier = Modifier
                .padding(RojanDimens.SpaceXS)
                .align(Alignment.TopEnd)
                .size(22.dp)
                .background(ManagerColors.BaseDeep.copy(alpha = 0.7f), CircleShape)
                .rojanPressable(onClick = onDeleteClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = "حذف تصویر", tint = ManagerColors.TextPrimary, modifier = Modifier.size(13.dp))
        }
        if (canMoveBack) {
            Box(
                modifier = Modifier
                    .padding(RojanDimens.SpaceXS)
                    .align(Alignment.BottomStart)
                    .size(22.dp)
                    .background(ManagerColors.BaseDeep.copy(alpha = 0.7f), CircleShape)
                    .rojanPressable(onClick = onMoveBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "جابجایی به عقب", tint = ManagerColors.TextPrimary, modifier = Modifier.size(13.dp))
            }
        }
        if (canMoveForward) {
            Box(
                modifier = Modifier
                    .padding(RojanDimens.SpaceXS)
                    .align(Alignment.BottomEnd)
                    .size(22.dp)
                    .background(ManagerColors.BaseDeep.copy(alpha = 0.7f), CircleShape)
                    .rojanPressable(onClick = onMoveForward),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = Icons.Filled.ArrowForward, contentDescription = "جابجایی به جلو", tint = ManagerColors.TextPrimary, modifier = Modifier.size(13.dp))
            }
        }
    }
}

@Composable
private fun AddTargetedMediaTile(onClick: () -> Unit) {
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

private fun gridHeightFor(itemCount: Int) = ((itemCount + 2) / 3 * 120).dp

private val PickVisualMediaImageOnlyRequest = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
