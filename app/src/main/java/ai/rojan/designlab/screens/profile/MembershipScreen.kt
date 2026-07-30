package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize

/**
 * Journey 2, Screen 6: Membership — progress-to-next-tier now reads
 * live shared state, updated by Appointment completion.
 */
@Composable
fun MembershipScreen(
    ecosystemViewModel: CustomerEcosystemViewModel,
    onBackClick: () -> Unit,
) {
    val tier = ecosystemViewModel.membershipTier()
    val remainingToNextTier = (tier.pointsToNextTier - ecosystemViewModel.state.membershipProgressPoints).coerceAtLeast(0)

    HomeBackgroundTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            GlassBackButton(onClick = onBackClick)
            Text("عضویت", style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary)

            HomeGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(RojanDimens.SpaceLG),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    RojanIconContainer(
    imageVector = Icons.Filled.WorkspacePremium,
    contentDescription = null,
    tint = HomeColors.Glow,
    size = RojanIconSize.XLarge,
)
                    Text("سطح ${tier.currentTierName}", style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary)
                    Text(
                        "$remainingToNextTier امتیاز تا سطح ${tier.nextTierName}",
                        style = RojanTypography.Body,
                        color = HomeColors.TextSecondary,
                    )
                }
            }

            Text("مزایای عضویت", style = RojanTypography.Body, color = HomeColors.TextPrimary)

            tier.benefits.forEachIndexed { index, benefit ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.rojanEnterAnimation(delayMillis = index * 60),
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = HomeColors.Glow, modifier = Modifier.size(RojanDimens.IconSizeSmall))
                    Text(" $benefit", style = RojanTypography.Body, color = HomeColors.TextPrimary)
                }
            }
        }
    }
}
