package ai.rojan.designlab.manager.components

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.repository.Banner
import ai.rojan.designlab.ui.components.image.RojanRemoteImage
import ai.rojan.designlab.ui.theme.RojanShapes
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/**
 * Manager Dashboard — admin-managed static banner (`BannerTarget.MANAGER`,
 * `ROJAN_Backend/PublicBannerController`,
 * `GET /api/v1/public/banners?target=MANAGER`, unauthenticated). Real,
 * remote artwork only — not a navigation mechanism: never clickable,
 * carries no title/subtitle/link handling even though the backend response
 * has those fields - any text the artwork itself contains (e.g. "با روژان
 * بیشتر آشنا شوید") is part of the image, not rendered or wired by this
 * component. Renders nothing when no active banner is configured, the
 * request fails, or the image fails to load - never a placeholder, a
 * visible error, or a retry affordance, same silent-failure convention
 * this dashboard's AIInsightCard already uses.
 */
@Composable
fun ManagerBannerSlot(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var banner by remember { mutableStateOf<Banner?>(null) }

    LaunchedEffect(Unit) {
        BackendApiContainerHolder.get(context).bannerRepository.getActiveManagerBanner()
            .onSuccess { banner = it }
    }

    banner?.let {
        RojanRemoteImage(
            url = it.imageUrl,
            contentDescription = null,
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            shape = RojanShapes.GlassCard,
            fallback = {},
        )
    }
}
