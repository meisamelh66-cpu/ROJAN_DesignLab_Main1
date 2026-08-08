package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.presentation.booking.BookingHistoryViewModel
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.screens.customer.hometheme.HomeCard
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Customer Home "Previous Salons".
 *
 * Production Data Integrity Phase 1: now backed by the real
 * `GET /api/v1/bookings/my` (via the shared [BookingHistoryViewModel]
 * `CustomerHomeScreen` hoists once for this section and [UpcomingBookings]
 * — one network call for both), filtered to
 * [ai.rojan.designlab.domain.repository.BookingStatus.COMPLETED] —
 * replaces the previous `CustomerEcosystemViewModel.state.pastAppointments`
 * demo read. No relative "days ago" label: that came from
 * `DemoAppointment.daysAgo`, a precomputed demo field with no backend
 * equivalent — the real booking date is shown instead of a fabricated
 * relative string.
 */
@Composable
fun RecentVisits(viewModel: BookingHistoryViewModel, onSalonClick: (String) -> Unit = {}) {
    val pastVisits = ((viewModel.state as? UiState.Success)?.data.orEmpty())
        .filter { it.booking.status == BookingStatus.COMPLETED }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        itemsIndexed(pastVisits) { index, item ->
            val booking = item.booking

            HomeCard(
                accentColor = HomeColors.Lavender,
                accentAlpha = 0.25f,
                onClick = { onSalonClick(booking.salonId) },
                width = 170.dp,
                height = 150.dp,
                index = index,
            ) {
                Text(
                    text = item.salonName ?: booking.salonId,
                    style = RojanTypography.Caption,
                    color = HomeColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                item.specialistName?.let { name ->
                    Text(
                        text = name,
                        style = RojanTypography.Caption,
                        color = HomeColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                ) {
                    RojanIconContainer(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = HomeColors.Primary,
                        size = RojanIconSize.Small,
                    )
                    Text(
                        text = booking.startTime.substringBefore('T'),
                        style = RojanTypography.Caption,
                        color = HomeColors.Primary,
                    )
                }
            }
        }
    }
}
