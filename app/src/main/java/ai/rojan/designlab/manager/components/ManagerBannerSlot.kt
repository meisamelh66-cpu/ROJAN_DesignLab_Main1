package ai.rojan.designlab.manager.components

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.ui.theme.RojanShapes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Manager Dashboard — admin-managed static banner slot, placed between the
 * Hero and Salon Identity. Real, remote artwork from ROJAN_Backend's Banner
 * Management (Super Admin), `target=MANAGER`
 * (`V43__manager_banner_target.sql`) — never fabricated, never a
 * placeholder image.
 *
 * DELIBERATELY NON-INTERACTIVE: this composable never applies
 * `rojanPressable` or any other click/tap modifier, never reads a `href`,
 * never triggers navigation of any kind. The backend's `Banner` entity
 * carries an `href` field shared by every target, but
 * [ai.rojan.designlab.domain.repository.Banner] (the domain type this
 * screen actually sees) doesn't even expose it — the artwork itself may
 * contain text such as "با روژان بیشتر آشنا شوید", but that is part of the
 * image's own pixels, not a UI affordance.
 *
 * Self-contained: fetches its own state directly from
 * [ai.rojan.designlab.di.BackendApiContainerHolder]'s `bannerRepository`,
 * completely independent of
 * [ai.rojan.designlab.manager.presentation.dashboard.ManagerDashboardViewModel] —
 * the existing dashboard's data/state/loading/error handling is entirely
 * untouched by this slot. Renders nothing at all when there is no active
 * banner, the fetch is still in flight, or it failed — this is a
 * decorative, admin-optional slot, not core salon data, so it never shows
 * a loading skeleton or error card the way the real dashboard sections do.
 */
@Composable
fun ManagerBannerSlot(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var imageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        BackendApiContainerHolder.get(context).bannerRepository
            .getActiveBanners("MANAGER")
            .onSuccess { banners -> imageUrl = banners.firstOrNull()?.imageUrl }
    }

    val url = imageUrl ?: return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2.5f)
            .shadow(
                elevation = 4.dp,
                shape = RojanShapes.GlassCard,
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.25f),
            )
            .clip(RojanShapes.GlassCard)
            .border(1.dp, ManagerColors.Turquoise.copy(alpha = 0.14f), RojanShapes.GlassCard),
    ) {
        // Static artwork only - no clickable modifier anywhere on this tree.
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
    }
}
