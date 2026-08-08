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
 * Journey 2, Screen 5: Coupons.
 *
 * Production Data Integrity Phase 1: gated. No coupons endpoint exists
 * anywhere in `data/remote/dto/` or `domain/repository/` — this was
 * entirely demo state ([ai.rojan.designlab.data.demo.DemoCouponRepository]
 * via [CustomerEcosystemViewModel]), including a "redeem" action that
 * computed its discount against a hardcoded reference price rather than a
 * real cart/booking context. Entry point stays reachable from Profile;
 * content becomes a Coming Soon state until a real coupons capability
 * exists on the backend.
 */
@Composable
fun CouponsScreen(
    onBackClick: () -> Unit,
) {
    HomeBackgroundTheme {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item { GlassBackButton(onClick = onBackClick) }
            item { Text("کدهای تخفیف", style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary) }
            item { RojanComingSoonState() }
        }
    }
}
