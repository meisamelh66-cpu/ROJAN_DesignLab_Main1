package ai.rojan.designlab.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ai.rojan.designlab.R

/**
 * The Hero Card's 3D illustration (calendar + flowers, per the approved
 * reference) — a plain [Image] at the correct size and position.
 *
 * Per project direction, 3D artwork is never recreated or simulated with
 * Compose drawing/gradients/shadows; it's always a real exported asset
 * (PNG/WebP from Spline/Blender/Icons8 3D/Figma 3D). [drawableRes] now
 * points at the approved real 3D export (`hero_calendar.webp`), which
 * replaces the earlier flat vector placeholder — no other code needs to
 * change.
 *
 * @param drawableRes drawable resource id for the illustration artwork.
 */
@Composable
fun IllustrationPlaceholder(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    drawableRes: Int = R.drawable.hero_calendar,
) {
    Image(
        painter = painterResource(id = drawableRes),
        contentDescription = "Hero illustration",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFEDE3FF)
@Composable
private fun IllustrationPlaceholderPreview() {
    IllustrationPlaceholder()
}