package ai.rojan.designlab.manager.screens.settings

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.components.ManagerTargetedMediaGallery
import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.domain.media.ManagerMediaType
import ai.rojan.designlab.manager.presentation.settings.ManagerSalonMediaViewModel
import ai.rojan.designlab.manager.presentation.settings.SalonMediaState
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.ui.components.image.RojanRemoteImage
import ai.rojan.designlab.ui.components.image.decodeResizeAndCompress
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
 *
 * Complete Salon Gallery UX (Media System Evolution v2): the gallery
 * section below is now [ManagerTargetedMediaGallery] - the same reusable
 * component the specialist-portfolio/service-images screens use - so
 * preview, reorder, and honest empty/loading states are shared, not
 * reimplemented per screen. Logo/cover keep their own `IdentityMediaCard`
 * (single-image "assign to a slot" semantics, a genuinely different
 * interaction from a many-image collection).
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

        ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
            ManagerTargetedMediaGallery(
                title = "گالری تصاویر",
                salonId = ManagerRepositories.salonId ?: "",
                mediaType = ManagerMediaType.GALLERY,
                targetId = null,
                repository = BackendApiContainerHolder.get(context).managerMediaRepository,
                modifier = Modifier.padding(RojanDimens.SpaceMD),
                maxDimension = GALLERY_MAX_DIMENSION,
            )
        }
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

private val PickVisualMediaImageOnlyRequest =
    androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

private const val LOGO_MAX_DIMENSION = 1024
private const val COVER_MAX_DIMENSION = 1600
private const val GALLERY_MAX_DIMENSION = 2048
