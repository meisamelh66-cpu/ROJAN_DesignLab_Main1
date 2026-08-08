package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Storefront
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.presentation.booking.BookingHistoryViewModel
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.theme.salonAccentColorFor

/**
 * Customer Home upcoming bookings.
 *
 * Production Data Integrity Phase 1: now backed by the real
 * `GET /api/v1/bookings/my` (via the shared [BookingHistoryViewModel]
 * `CustomerHomeScreen` hoists once for this section and [RecentVisits] —
 * one network call for both, not two) — replaces the previous
 * `CustomerEcosystemViewModel.state.upcomingAppointments`/`CatalogEngine`
 * demo read. No service name/price shown: `Booking` has no service-by-id
 * lookup available (see `BookingHistoryRepository`'s doc comment) — a
 * documented backend gap, not silently dropped.
 */
@Composable
fun UpcomingBookings(viewModel: BookingHistoryViewModel) {
    val upcoming = ((viewModel.state as? UiState.Success)?.data.orEmpty())
        .filter { it.booking.status == BookingStatus.PENDING || it.booking.status == BookingStatus.CONFIRMED }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        itemsIndexed(upcoming) { index, item ->
            val booking = item.booking

            Box(
                modifier = Modifier
                    .width(220.dp)
                    .background(HomeColors.Primary.copy(alpha = 0.18f), RojanShapes.Small)
            ) {
                HomeGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp)
                        .rojanPressable(onClick = {}),
                    shape = RojanShapes.Small,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RojanDimens.SpaceSM),
                        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(salonAccentColorFor(booking.salonId).copy(alpha = 0.5f), RojanShapes.Small),
                            contentAlignment = Alignment.Center,
                        ) {
                            RojanIconContainer(
                                imageVector = Icons.Filled.Storefront,
                                contentDescription = null,
                                tint = HomeColors.TextPrimary,
                                size = RojanIconSize.Medium,
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
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
                                    tint = HomeColors.TextSecondary,
                                    size = RojanIconSize.Small,
                                )
                                Text(
                                    text = booking.startTime.substringBefore('T'),
                                    style = RojanTypography.Caption,
                                    color = HomeColors.TextSecondary,
                                )
                                RojanIconContainer(
                                    imageVector = Icons.Filled.AccessTime,
                                    contentDescription = null,
                                    tint = HomeColors.TextSecondary,
                                    size = RojanIconSize.Small,
                                )
                                Text(
                                    text = booking.startTime.substringAfter('T').take(5),
                                    style = RojanTypography.Caption,
                                    color = HomeColors.TextSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
