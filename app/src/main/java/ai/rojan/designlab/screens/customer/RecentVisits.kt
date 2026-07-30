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

import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.screens.customer.hometheme.HomeCard
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize

/**
 * Customer Home "Previous Salons" — UX Refactor Phase 1 relabeling of
 * what was internally still "recent visits."
 *
 * Code Cleanup pass: migrated off its previous local `fakeRecentVisits`
 * list onto [CustomerEcosystemViewModel.state]'s real
 * [ai.rojan.designlab.domain.customer.CustomerEcosystemState.pastAppointments] —
 * the same real completed-appointment data
 * [ai.rojan.designlab.screens.profile.AppointmentsScreen] already reads.
 * "زمان نسبی" (relative time) now comes from real
 * [ai.rojan.designlab.data.demo.DemoAppointment.daysAgo] instead of a
 * separate hand-written string per fake entry.
 *
 * UX Refactor Phase 1: [onSalonClick] navigates to that appointment's
 * salon via [ai.rojan.designlab.data.demo.DemoAppointment.salonId] — a
 * no-op for the rare pre-existing appointment predating that field.
 */
@Composable
fun RecentVisits(ecosystemViewModel: CustomerEcosystemViewModel, onSalonClick: (String) -> Unit = {}) {
    val pastVisits = ecosystemViewModel.state.pastAppointments

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        itemsIndexed(pastVisits) { index, visit ->
            HomeCard(
                accentColor = HomeColors.Lavender,
                accentAlpha = 0.25f,
                onClick = { visit.salonId?.let(onSalonClick) },
                width = 170.dp,
                height = 150.dp,
                index = index,
            ) {
                Text(
                    text = visit.salonName,
                    style = RojanTypography.Caption,
                    color = HomeColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = "${visit.serviceName} · ${visit.specialistName}",
                    style = RojanTypography.Caption,
                    color = HomeColors.TextSecondary,
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
                    color = HomeColors.TextSecondary,
                )

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
                        text = "رزرو مجدد",
                        style = RojanTypography.Caption,
                        color = HomeColors.Primary,
                    )
                }
            }
        }
    }
}
