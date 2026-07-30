package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.data.demo.DemoLoyaltyEntry
import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanStatusOnline
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.theme.RojanVividMagenta

/**
 * Journey 2, Screen 7: Loyalty — points total now reads live shared
 * state, so points earned via Appointment completion appear here
 * immediately. The entry history list itself remains the static demo
 * set (the engine doesn't append a loyalty-history line item the same
 * way it does for wallet transactions — disclosed simplification, not
 * silently incomplete).
 */
@Composable
fun LoyaltyScreen(
    ecosystemViewModel: CustomerEcosystemViewModel,
    onBackClick: () -> Unit,
) {
    val state = ecosystemViewModel.state

    HomeBackgroundTheme {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item { GlassBackButton(onClick = onBackClick) }
            item { Text("امتیازات وفاداری", style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary) }

            item {
                HomeGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RojanDimens.SpaceLG),
                    ) {
                        Icon(Icons.Filled.Stars, contentDescription = null, tint = HomeColors.Glow, modifier = Modifier.size(40.dp))
                        Text("${state.loyaltyPoints} امتیاز", style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary)
                    }
                }
            }

            item { Text("تاریخچه امتیازات", style = RojanTypography.Body, color = HomeColors.TextPrimary) }

            itemsIndexed(ecosystemViewModel.allLoyaltyEntries()) { index, entry ->
                LoyaltyEntryRow(entry, animationDelayMillis = index * 60)
            }
        }
    }
}

@Composable
private fun LoyaltyEntryRow(entry: DemoLoyaltyEntry, animationDelayMillis: Int = 0) {
    HomeGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .rojanEnterAnimation(delayMillis = animationDelayMillis),
        shape = RojanShapes.Small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.title, style = RojanTypography.Body, color = HomeColors.TextPrimary)
                Text(entry.dateLabel, style = RojanTypography.Caption, color = HomeColors.TextSecondary)
            }
            Text(
                text = "${if (entry.isEarned) "+" else ""}${entry.points}",
                style = RojanTypography.Body,
                color = if (entry.isEarned) RojanStatusOnline else RojanVividMagenta,
            )
        }
    }
}
