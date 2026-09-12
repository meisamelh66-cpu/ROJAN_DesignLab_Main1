package ai.rojan.designlab.ui.components.image

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

import ai.rojan.designlab.ui.theme.RojanShapes

/**
 * Salon Discovery: the first real remote-image (Coil) rendering in this
 * app. Every other logo/avatar slot ([RojanSampleImage]/[SpecialistAvatar])
 * deliberately stayed a color-tinted icon placeholder because no
 * image-loading library existed yet (see their own doc comments) - this
 * is that seam, added specifically for backend-real `logoUrl`/`photoUrl`
 * fields that already exist on the wire (`Salon.logoUrl`,
 * `Specialist.photoUrl`) but were never rendered.
 *
 * [url] null, blank, or a failed load all resolve to [fallback] - never a
 * broken-image glyph or an indefinite blank box. Center-cropped, clipped
 * to [shape] to match every other image primitive in this app.
 */
@Composable
fun RojanRemoteImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RojanShapes.Small,
    fallback: @Composable () -> Unit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (url.isNullOrBlank()) {
            fallback()
        } else {
            var failed by remember(url) { mutableStateOf(false) }
            if (failed) {
                fallback()
            } else {
                val context = LocalContext.current
                // Engineering Cleanup Phase 4 (P2): the request is `remember`ed
                // keyed on the url so a recomposition (scroll, follow-state
                // toggle, parent state change) reuses the same instance instead
                // of allocating a fresh builder+request every frame. Coil sizes
                // the bitmap from this composable's measured bounds
                // automatically — the `Box(modifier)` is always bounded by the
                // caller (72dp logo / 64–88dp avatar), so no explicit `.size()`
                // is needed or wanted (that would defeat the downsampling).
                val request = remember(url, context) {
                    ImageRequest.Builder(context).data(url).crossfade(true).build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    onError = { failed = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape),
                )
            }
        }
    }
}
