package ai.rojan.designlab.manager.screens.booking

import ai.rojan.designlab.domain.repository.Service
import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerEmptyState
import ai.rojan.designlab.manager.components.ManagerErrorState
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerLoadingState
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.data.formatDurationMinutes
import ai.rojan.designlab.manager.data.formatTomanPrice
import ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModel
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToLong

/**
 * Manager Booking Journey — step 2: pick the service.
 *
 * **Manager Booking Creation Integrity follow-up:** sources the salon's
 * real service catalog via [ManagerBookingViewModel.catalogState]
 * (`GET /salons/{salonId}/categories` + `.../services`) — the same
 * repositories the Customer booking flow already uses — replacing
 * `ManagerRepositories.services`' in-memory sample list. A real
 * [Service] has no "category name" field the way the old demo model
 * did (categories are a separate resource); this screen shows duration
 * and price only.
 */
@Composable
fun ManagerBookingServiceScreen(
    viewModel: ManagerBookingViewModel,
    onBackClick: (() -> Unit)? = null,
    onServiceSelected: () -> Unit = {},
) {
    ManagerScaffold(onBackClick = onBackClick) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item {
                RtlSectionHeader(
                    text = "انتخاب خدمت",
                    style = RojanTypography.ScreenTitle,
                    color = ManagerColors.TextPrimary,
                    horizontalPadding = 0.dp,
                )
            }

            when (val catalogState = viewModel.catalogState) {
                is UiState.Loading -> item { ManagerLoadingState(message = "در حال بارگذاری خدمات...") }
                is UiState.Error -> item {
                    ManagerErrorState(
                        description = catalogState.message,
                        actionLabel = "تلاش مجدد",
                        onAction = { viewModel.retryLoadCatalog() },
                    )
                }
                is UiState.Empty -> item {
                    ManagerEmptyState(
                        title = "هنوز سالنی ثبت نکرده‌اید",
                        description = "برای رزرو نوبت، ابتدا باید یک سالن برای حساب کاربری خود ثبت کنید.",
                    )
                }
                is UiState.Success -> {
                    val services = catalogState.data.services
                    if (services.isEmpty()) {
                        item { ManagerEmptyState(title = "هنوز خدمتی ثبت نشده است") }
                    } else {
                        items(services) { service ->
                            BookingServiceRow(
                                service = service,
                                onClick = {
                                    viewModel.selectService(service.id)
                                    onServiceSelected()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingServiceRow(service: Service, onClick: () -> Unit) {
    ManagerGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = service.name, style = RojanTypography.Body, color = ManagerColors.TextPrimary)
                Text(
                    text = formatDurationMinutes(service.durationMinutes),
                    style = RojanTypography.Caption,
                    color = ManagerColors.TextSecondary,
                )
                Text(
                    text = formatTomanPrice(service.price.roundToLong()),
                    style = RojanTypography.Caption,
                    color = ManagerColors.GoldLight,
                )
            }
        }
    }
}
