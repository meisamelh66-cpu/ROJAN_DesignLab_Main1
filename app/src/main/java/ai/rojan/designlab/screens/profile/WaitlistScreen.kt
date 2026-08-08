package ai.rojan.designlab.screens.profile

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
 * "My Waiting List".
 *
 * Production Data Integrity Phase 1: gated. No waitlist endpoint exists
 * anywhere in `data/remote/dto/` or `domain/repository/` — join/leave
 * were entirely demo/in-memory actions
 * ([ai.rojan.designlab.data.demo.DemoWaitlistEntry] via
 * [CustomerEcosystemViewModel]). Entry point stays reachable; content
 * becomes a Coming Soon state until a real waitlist capability exists on
 * the backend. `BookingTimeScreen.kt`'s "Join Waiting List" prompt (the
 * other consumer of this same feature) is gated alongside this screen.
 */
@Composable
fun WaitlistScreen(
    onBackClick: () -> Unit,
) {
    HomeBackgroundTheme {
        Column(modifier = Modifier.fillMaxSize().padding(RojanDimens.SpaceMD)) {
            GlassBackButton(onClick = onBackClick)

            Text(
                text = "لیست انتظار من",
                style = RojanTypography.HeroTitle,
                color = HomeColors.TextPrimary,
                modifier = Modifier.padding(vertical = RojanDimens.SpaceMD),
            )

            RojanComingSoonState()
        }
    }
}
