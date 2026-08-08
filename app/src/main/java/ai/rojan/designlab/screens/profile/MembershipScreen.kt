package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
 * Journey 2, Screen 6: Membership.
 *
 * Production Data Integrity Phase 1: gated. No membership-tier endpoint
 * exists anywhere in `data/remote/dto/` or `domain/repository/` — this
 * was entirely demo/in-memory state
 * ([ai.rojan.designlab.data.demo.DemoMembershipRepository] via
 * [CustomerEcosystemViewModel]). Entry point stays reachable from Profile;
 * content becomes a Coming Soon state until a real membership capability
 * exists on the backend.
 */
@Composable
fun MembershipScreen(
    onBackClick: () -> Unit,
) {
    HomeBackgroundTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            GlassBackButton(onClick = onBackClick)
            Text("عضویت", style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary)
            RojanComingSoonState()
        }
    }
}
