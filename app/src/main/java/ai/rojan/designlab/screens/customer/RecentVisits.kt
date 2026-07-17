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
import ai.rojan.designlab.ui.theme.RojanSoftLavender
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.theme.RojanVividPurple
import ai.rojan.designlab.ui.components.icon.RojanIconContainer

/**
 * Customer Home recent visits.
 *
 * Code Cleanup pass: migrated off its previous local `fakeRecentVisits`
 * list onto [CustomerEcosystemViewModel.state]'s real
 * [ai.rojan.designlab.domain.customer.CustomerEcosystemState.pastAppointments] —
 * the same real completed-appointment data
 * [ai.rojan.designlab.screens.profile.AppointmentsScreen] already reads.
 * "زمان نسبی" (relative time) now comes from real
 * [ai.rojan.designlab.data.demo.DemoAppointment.daysAgo] instead of a
 * separate hand-written string per fake entry.
 */
@Composable
fun RecentVisits(ecosystemViewModel: CustomerEcosystemViewModel) {
    val pastVisits = ecosystemViewModel.state.pastAppointments

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        items(pastVisits) { visit ->
            Box(
                modifier = Modifier
                    .size(width = 170.dp, height = 150.dp)
                    .background(RojanSoftLavender.copy(alpha = 0.25f), RojanShapes.Small)
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
                        Text(
                            text = visit.salonName,
                            style = RojanTypography.Caption,
                            color = RojanTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Text(
                            text = "${visit.serviceName} · ${visit.specialistName}",
                            style = RojanTypography.Caption,
                            color = RojanTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Text(
                            text = visit.daysAgo?.let { days ->
                                when {
                                    days == 0 -> "امروز"
                                    days < 7 -> "$days روز پیش"
                                    days < 30 -> "${days / 7} هفته پیش"
                                    else -> "${days / 30} ماه پیش"
                                }
                            } ?: "",
                            style = RojanTypography.Caption,
                            color = RojanTextSecondary,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                        ) {
                            RojanIconContainer(
    imageVector = Icons.Filled.CalendarMonth,
    contentDescription = null,
    tint = RojanVividPurple,
    sizeOverride = 14.dp,
)
                            Text(
                                text = "رزرو مجدد",
                                style = RojanTypography.Caption,
                                color = RojanVividPurple,
                            )
                        }
                    }
                }
            }
        }
    }
}
