package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import ai.rojan.designlab.data.demo.DemoWaitlistEntry
import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Appointment System completion (V1.0 Module 6 - Waiting List). "My
 * Waiting List" — shows every currently-[ai.rojan.designlab.data.demo.WaitlistStatus.WAITING]
 * entry (via [CustomerEcosystemViewModel.state.activeWaitlistEntries],
 * already filtered), ordered by [DemoWaitlistEntry.requestedAtSequence]
 * so the customer sees their real FIFO position, with a real Leave
 * action per entry.
 */
@Composable
fun WaitlistScreen(
    ecosystemViewModel: CustomerEcosystemViewModel,
    onBackClick: () -> Unit,
) {
    val entries = ecosystemViewModel.state.activeWaitlistEntries.sortedBy { it.requestedAtSequence }

    HomeBackgroundTheme {
        Column(modifier = Modifier.fillMaxSize().padding(RojanDimens.SpaceMD)) {
            GlassBackButton(onClick = onBackClick)

            Text(
                text = "لیست انتظار من",
                style = RojanTypography.HeroTitle,
                color = HomeColors.TextPrimary,
                modifier = Modifier.padding(vertical = RojanDimens.SpaceMD),
            )

            if (entries.isEmpty()) {
                Text(
                    text = "شما در حال حاضر در هیچ لیست انتظاری نیستید",
                    style = RojanTypography.Body,
                    color = HomeColors.TextSecondary,
                )
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                itemsIndexed(entries) { index, entry ->
                    val position = entries.indexOf(entry) + 1
                    WaitlistEntryCard(
                        entry = entry,
                        position = position,
                        onLeave = { ecosystemViewModel.leaveWaitlist(entry.id) },
                        animationDelayMillis = index * 60,
                    )
                }
            }
        }
    }
}

@Composable
private fun WaitlistEntryCard(
    entry: DemoWaitlistEntry,
    position: Int,
    onLeave: () -> Unit,
    animationDelayMillis: Int = 0,
) {
    HomeGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .rojanEnterAnimation(delayMillis = animationDelayMillis),
        shape = RojanShapes.Small,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(entry.salonName, style = RojanTypography.Body, color = HomeColors.TextPrimary)
                    Text(
                        "${entry.serviceName} • ${entry.dateLabel}",
                        style = RojanTypography.Caption,
                        color = HomeColors.TextSecondary,
                    )
                }
                Text(
                    text = "جایگاه $position",
                    style = RojanTypography.Caption,
                    color = HomeColors.TextSecondary,
                )
            }

            Text(
                text = "خروج از لیست انتظار",
                style = RojanTypography.Caption,
                color = RojanErrorText,
                modifier = Modifier
                    .padding(top = RojanDimens.SpaceSM)
                    .clickable(onClick = onLeave),
            )
        }
    }
}
