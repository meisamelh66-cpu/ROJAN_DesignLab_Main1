package ai.rojan.designlab.ui.components.image

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.animation.RojanAnimations
import ai.rojan.designlab.ui.theme.RojanTextPrimary

/**
 * ROJAN specialist avatar — the one place every specialist's profile
 * photo renders through, across Home (Top Specialists), Salon Details,
 * Specialist Selection, and Specialist Profile. Callers only ever pass
 * [assetRes] (from [ai.rojan.designlab.data.demo.DemoSpecialist.assetRes],
 * itself sourced from [ai.rojan.designlab.ui.assets.SampleImageProvider.forSpecialist])
 * — this is the reusable seam: when real per-specialist backend photos
 * arrive, only [SampleImageProvider.forSpecialist]'s return value changes,
 * not this component or any of its call sites.
 *
 * Always circular, always center-cropped (via [RojanSampleImage]'s own
 * fixed `ContentScale.Crop`), and always sized by the caller's own
 * `modifier` — this component owns none of the surrounding box size or
 * background tint each screen already has; it only fills whatever slot
 * it's given. Falls back to a plain [Icons.Filled.Person] glyph when
 * [assetRes] is null, matching every other image-card component in this
 * app ([ai.rojan.designlab.ui.components.image.RojanSampleImage]'s own
 * consumers all follow the same asset-or-icon pattern).
 *
 * Plays a one-time fade-in (0 -> 1 alpha) whenever a real image is shown,
 * keyed on [assetRes] so a different specialist's photo re-triggers it.
 */
@Composable
fun SpecialistAvatar(
    assetRes: Int?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallbackIconSize: Dp = 24.dp,
    photoUrl: String? = null,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (!photoUrl.isNullOrBlank()) {
            // Salon Discovery: the real seam this class's own doc comment
            // anticipated - a real per-specialist backend photo (`Specialist.photoUrl`)
            // now renders via Coil, still falling back to the same
            // assetRes/icon path below on a null/blank/failed url.
            ai.rojan.designlab.ui.components.image.RojanRemoteImage(
                url = photoUrl,
                contentDescription = contentDescription,
                shape = CircleShape,
                modifier = Modifier.fillMaxSize(),
                fallback = {
                    if (assetRes != null) {
                        RojanSampleImage(
                            resId = assetRes,
                            contentDescription = contentDescription,
                            shape = CircleShape,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = contentDescription,
                            tint = RojanTextPrimary,
                            modifier = Modifier.size(fallbackIconSize),
                        )
                    }
                },
            )
        } else if (assetRes != null) {
            var loaded by remember(assetRes) { mutableStateOf(false) }
            LaunchedEffect(assetRes) { loaded = true }
            val alpha by animateFloatAsState(
                targetValue = if (loaded) 1f else 0f,
                animationSpec = RojanAnimations.ContentEnterSpec,
                label = "specialist_avatar_fade_in",
            )
            RojanSampleImage(
                resId = assetRes,
                contentDescription = contentDescription,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = contentDescription,
                tint = RojanTextPrimary,
                modifier = Modifier.size(fallbackIconSize),
            )
        }
    }
}
