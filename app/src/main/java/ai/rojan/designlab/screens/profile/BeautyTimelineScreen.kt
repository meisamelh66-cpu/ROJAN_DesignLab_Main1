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
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.data.demo.DemoBeautyTimelineEntry
import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Journey 2, Screen 9: Beauty Timeline — real shared mutable state (item
 * 7 of the Journey 2 completion checklist). Completing an appointment
 * via [ai.rojan.designlab.screens.profile.AppointmentsScreen] genuinely
 * adds a new entry here through
 * [ai.rojan.designlab.domain.customer.CustomerEcosystemEngine] →
 * [ai.rojan.designlab.domain.customer.EcosystemEventReducer], not a
 * static list anymore.
 */
@Composable
fun BeautyTimelineScreen(
    ecosystemViewModel: CustomerEcosystemViewModel,
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
            item { Text("تاریخچه زیبایی", style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary) }

            itemsIndexed(ecosystemViewModel.state.beautyTimelineEntries) { index, entry ->
                TimelineCard(entry, animationDelayMillis = index * 60)
            }
        }
    }
}

@Composable
private fun TimelineCard(entry: DemoBeautyTimelineEntry, animationDelayMillis: Int = 0) {
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
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Filled.History, contentDescription = null, tint = HomeColors.Glow, modifier = Modifier.size(RojanDimens.IconSizeMedium))
            Column(modifier = Modifier.padding(start = RojanDimens.SpaceSM)) {
                Text(entry.serviceName, style = RojanTypography.Body, color = HomeColors.TextPrimary)
                Text("${entry.salonName} • ${entry.dateLabel}", style = RojanTypography.Caption, color = HomeColors.TextSecondary)
                Text(entry.note, style = RojanTypography.Caption, color = HomeColors.TextSecondary)
            }
        }
    }
}
