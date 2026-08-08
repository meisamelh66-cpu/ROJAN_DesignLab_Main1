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
 * Journey 2, Screen 7: Loyalty.
 *
 * Production Data Integrity Phase 1: gated. No loyalty-points endpoint
 * exists anywhere in `data/remote/dto/` or `domain/repository/` — this
 * was entirely demo/in-memory state
 * ([ai.rojan.designlab.data.demo.DemoLoyaltyRepository] via
 * [CustomerEcosystemViewModel]). Entry point stays reachable from Profile;
 * content becomes a Coming Soon state until a real loyalty capability
 * exists on the backend.
 */
@Composable
fun LoyaltyScreen(
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
            item { Text("امتیازات وفاداری", style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary) }
            item { RojanComingSoonState() }
        }
    }
}
