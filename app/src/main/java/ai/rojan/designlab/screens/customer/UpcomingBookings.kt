package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.theme.RojanVividPurple
import ai.rojan.designlab.ui.components.icon.RojanIconContainer

/**
 * Customer Home upcoming bookings.
 *
 * Code Cleanup pass: migrated off its previous local `fakeUpcomingBookings`
 * list onto [CustomerEcosystemViewModel.state]'s real
 * [ai.rojan.designlab.domain.customer.CustomerEcosystemState.upcomingAppointments] —
 * the same real data [ai.rojan.designlab.screens.profile.AppointmentsScreen]
 * already reads, reconciling what used to be a separate, duplicate demo
 * dataset with a different set of fake names.
 */
@Composable
fun UpcomingBookings(ecosystemViewModel: CustomerEcosystemViewModel) {
    val upcoming = ecosystemViewModel.state.upcomingAppointments

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        items(upcoming) { booking ->
            Box(
                modifier = Modifier
                    .size(width = 190.dp, height = 150.dp)
                    .background(RojanVividPurple.copy(alpha = 0.18f), RojanShapes.Small)
            ) {
                GlassSurface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { },
                    shape = RojanShapes.Small,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RojanDimens.SpaceSM),
                        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(RojanVividPurple, CircleShape)
                            )
                            Text(
                                text = "تایید شده",
                                style = RojanTypography.Caption,
                                color = RojanVividPurple,
                            )
                        }

                        Text(
                            text = booking.salonName,
                            style = RojanTypography.Caption,
                            color = RojanTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Text(
                            text = "${booking.serviceName} · ${booking.specialistName}",
                            style = RojanTypography.Caption,
                            color = RojanTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                        ) {
                            RojanIconContainer(
    imageVector = Icons.Filled.CalendarMonth,
    contentDescription = null,
    tint = RojanTextSecondary,
    sizeOverride = 14.dp,
)
                            Text(
                                text = "${booking.dateLabel}، ${booking.time}",
                                style = RojanTypography.Caption,
                                color = RojanTextSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}
