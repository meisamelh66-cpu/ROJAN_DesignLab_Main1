package ai.rojan.designlab.ui.components.image

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ai.rojan.designlab.ui.components.interaction.rojanPressable

/**
 * Complete Salon Gallery UX (Media System Evolution v2): full-screen,
 * swipeable image preview - the "preview" requirement for every gallery
 * this app renders (salon gallery, specialist portfolio, service images).
 * One shared component rather than three copies, since the interaction
 * (tap thumbnail -> full-screen, swipe between images, tap to dismiss) is
 * identical regardless of which media collection is being viewed.
 *
 * [urls] the full set being previewed, [initialIndex] which one was tapped.
 * Edge-to-edge black scrim (not the app's glass theme) - a photo viewer, not
 * a themed screen. Optimized-image handling is [RojanRemoteImage]'s own
 * (Coil disk+memory caching, same as every thumbnail) - this dialog doesn't
 * re-fetch anything the grid didn't already load.
 */
@Composable
fun MediaPreviewDialog(
    urls: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    if (urls.isEmpty()) return
    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, urls.lastIndex)) { urls.size }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                RojanRemoteImage(
                    url = urls[page],
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    fallback = {},
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    .rojanPressable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "بستن", tint = Color.White)
            }
        }
    }
}
