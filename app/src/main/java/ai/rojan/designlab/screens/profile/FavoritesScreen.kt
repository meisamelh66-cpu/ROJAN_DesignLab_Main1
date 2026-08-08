package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.state.RojanComingSoonState
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Journey 2, Screen 3: Favorites.
 *
 * Production Data Integrity Phase 1: gated. The follow/favorite toggle
 * this screen listed (`CustomerEcosystemViewModel.state.favoriteSalonIds`)
 * has no backend persistence anywhere — no favorite/follow endpoint exists
 * on `SalonApi`, and the underlying state was in-memory, per-session only,
 * literally defaulting from `DemoSalonRepository.salons.map { it.id }`.
 * Migrating just the salon *read* while keeping that fake toggle would
 * still be simulating a feature the backend doesn't support (see the same
 * reasoning applied to `FollowedSalons.kt` and the follow toggle removed
 * from `SalonDetailsScreen.kt`). Entry point stays reachable from Profile;
 * content becomes a Coming Soon state until the backend adds real
 * favorite/follow persistence.
 */
@Composable
fun FavoritesScreen(
    onBackClick: () -> Unit,
    onSalonClick: (String) -> Unit,
) {
    HomeBackgroundTheme {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item { GlassBackButton(onClick = onBackClick) }
            item { Text("سالن‌های دنبال‌شده", style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary) }
            item { RojanComingSoonState() }
        }
    }
}
