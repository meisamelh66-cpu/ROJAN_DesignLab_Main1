package ai.rojan.designlab.manager.screens.booking

import ai.rojan.designlab.domain.repository.Specialist
import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerEmptyState
import ai.rojan.designlab.manager.components.ManagerErrorState
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerLoadingState
import ai.rojan.designlab.manager.components.ManagerScaffold
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

/**
 * Manager Booking Journey — step 3: pick the specialist.
 *
 * **Manager Booking Creation Integrity follow-up:** sources the salon's
 * real specialist roster via [ManagerBookingViewModel.catalogState]
 * (`GET /salons/{salonId}/specialists`) — replacing
 * `ManagerRepositories.specialists`' in-memory sample list. The previous
 * "specialists whose skills cover the selected service" filter is gone:
 * a real backend [Specialist] has no skills/services-offered concept to
 * filter on (disclosed simplification, not a silently dropped feature —
 * see `TEAM2_RESULT_MANAGER_BOOKING_CREATION_V2.md`). Every specialist at
 * the salon is shown; the backend itself still rejects a specialist/
 * service combination that doesn't make sense when the booking is
 * actually submitted.
 */
@Composable
fun ManagerBookingSpecialistScreen(
    viewModel: ManagerBookingViewModel,
    onBackClick: (() -> Unit)? = null,
    onSpecialistSelected: () -> Unit = {},
) {
    ManagerScaffold(onBackClick = onBackClick) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item {
                RtlSectionHeader(
                    text = "انتخاب متخصص",
                    style = RojanTypography.ScreenTitle,
                    color = ManagerColors.TextPrimary,
                    horizontalPadding = 0.dp,
                )
            }

            when (val catalogState = viewModel.catalogState) {
                is UiState.Loading -> item { ManagerLoadingState(message = "در حال بارگذاری متخصصان...") }
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
                    val specialists = catalogState.data.specialists
                    if (specialists.isEmpty()) {
                        item { ManagerEmptyState(title = "هنوز متخصصی ثبت نشده است") }
                    } else {
                        items(specialists) { specialist ->
                            BookingSpecialistRow(
                                specialist = specialist,
                                onClick = {
                                    viewModel.selectSpecialist(specialist.id)
                                    onSpecialistSelected()
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
private fun BookingSpecialistRow(specialist: Specialist, onClick: () -> Unit) {
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
                Text(text = specialist.displayName, style = RojanTypography.Body, color = ManagerColors.TextPrimary)
                if (specialist.bio != null) {
                    Text(
                        text = specialist.bio,
                        style = RojanTypography.Caption,
                        color = ManagerColors.TextSecondary,
                    )
                }
            }
        }
    }
}
