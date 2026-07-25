package ai.rojan.designlab.ui.components.image

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

import ai.rojan.designlab.ui.theme.RojanShapes

/**
 * Production Asset Normalization — the single primitive every sample
 * image "card" slot in the app renders through, so aspect ratio, crop
 * style, corner radius, and loading behavior can never drift apart
 * between salon/specialist/service/product/gallery cards the way the
 * previous per-screen inline `Image(...)` calls had started to (some
 * clipped to their container's shape, some didn't; the image itself was
 * never actually clipped in most of them, only the background behind
 * it).
 *
 * Deliberately narrow, matching [ai.rojan.designlab.ui.components.icon.RojanIconContainer]'s
 * own scope: this owns *rendering* concerns only (crop, clip, loading)
 * — the resource id itself always comes from
 * [ai.rojan.designlab.ui.assets.SampleImageProvider], never a raw
 * `R.drawable.*` at the call site.
 *
 * [ContentScale.Crop] is not a parameter — every card on every screen
 * this pass touched already used Crop, and "same crop style" is one of
 * this pass's explicit requirements, so it's fixed here rather than left
 * open to a future inconsistent choice. [shape] defaults to
 * [RojanShapes.Small] (every rectangular photo card in the app already
 * used this radius) and is overridable for the one real exception,
 * specialist avatars, which are circular.
 */
@Composable
fun RojanSampleImage(
    @DrawableRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RojanShapes.Small,
) {
    Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(shape),
    )
}
