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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.ui.background.PremiumBackground
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
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

    PremiumBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            GlassBackButton(onClick = onBackClick)
            Text("عضویت", style = RojanTypography.HeroTitle, color = RojanTextOnGlass)

            GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(RojanDimens.SpaceLG),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    RojanIconContainer(
    imageVector = Icons.Filled.WorkspacePremium,
    contentDescription = null,
    tint = RojanAIGlow,
    size = RojanIconSize.XLarge,
)
                    Text("سطح ${tier.currentTierName}", style = RojanTypography.HeroTitle, color = RojanTextPrimary)
                    Text(
                        "$remainingToNextTier امتیاز تا سطح ${tier.nextTierName}",
                        style = RojanTypography.Body,
                        color = RojanTextSecondary,
                    )
                }
            }

            Text("مزایای عضویت", style = RojanTypography.Body, color = RojanTextOnGlass)

            tier.benefits.forEach { benefit ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = RojanAIGlow, modifier = Modifier.size(18.dp))
                    Text(" $benefit", style = RojanTypography.Body, color = RojanTextPrimary)
                }
            }
        }
    }
}
