package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Storefront
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.components.image.RojanSampleImage
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize

/**
 * Customer Home upcoming bookings — Home Visual Language Unification.
 *
 * Same horizontal scrolling list, same real data
 * ([CustomerEcosystemViewModel.state.upcomingAppointments]) and same
 * position on Home as before. Card anatomy upgraded to match the
 * reference's "نوبت بعدی شما" card — a real salon photo (via
 * [CatalogEngine], same lookup [ai.rojan.designlab.screens.profile.AppointmentsScreen]
 * already does) and a decorative overflow glyph — while staying a
 * compact horizontal-scroll item, not the reference's single full-width
 * card, since this section shows potentially several upcoming
 * appointments, not exactly one.
 */
@Composable
fun UpcomingBookings(ecosystemViewModel: CustomerEcosystemViewModel) {
    val upcoming = ecosystemViewModel.state.upcomingAppointments
    val catalogEngine = remember { CatalogEngine() }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        itemsIndexed(upcoming) { index, booking ->
            val salon = booking.salonId?.let { catalogEngine.findSalonById(it) }

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
                                .background((salon?.colorSeed ?: HomeColors.Primary).copy(alpha = 0.5f), RojanShapes.Small),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (salon?.assetRes != null) {
                                RojanSampleImage(
                                    resId = salon.assetRes,
                                    contentDescription = booking.salonName,
                                    shape = RojanShapes.Small,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                RojanIconContainer(
                                    imageVector = Icons.Filled.Storefront,
                                    contentDescription = null,
                                    tint = HomeColors.TextPrimary,
                                    size = RojanIconSize.Medium,
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(HomeColors.Primary, CircleShape)
                                )
                                RojanIconContainer(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = null,
                                    tint = HomeColors.TextSecondary,
                                    size = RojanIconSize.Small,
                                )
                            }

                            Text(
                                text = booking.serviceName,
                                style = RojanTypography.Caption,
                                color = HomeColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            Text(
                                text = booking.salonName,
                                style = RojanTypography.Caption,
                                color = HomeColors.TextSecondary,
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
                                    tint = HomeColors.TextSecondary,
                                    size = RojanIconSize.Small,
                                )
                                Text(
                                    text = booking.dateLabel,
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
                                    text = booking.time,
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
