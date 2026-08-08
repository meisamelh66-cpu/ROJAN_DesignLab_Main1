package ai.rojan.designlab.manager.screens.booking

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.domain.specialist.Specialist
import ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModel
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Manager Booking Journey Phase 2 — step 3: pick the specialist.
 * [ManagerBookingViewModel.specialistsFor] prefers specialists whose
 * skills cover the already-selected service, falling back to the full
 * roster rather than dead-ending on an empty list. Since Phase 2 (M3),
 * [ai.rojan.designlab.manager.domain.specialist.Specialist.skills] is
 * always empty for real backend data (see
 * [ai.rojan.designlab.manager.data.BackendSpecialistRepository]'s own
 * doc comment), so this always takes that fallback today — every active
 * specialist shows, none is filtered out.
 */
@Composable
fun ManagerBookingSpecialistScreen(
    viewModel: ManagerBookingViewModel,
    onBackClick: (() -> Unit)? = null,
    onSpecialistSelected: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedServiceName = remember(state.serviceId) { viewModel.serviceById(state.serviceId)?.name }
    val specialists = remember(selectedServiceName) { viewModel.specialistsFor(selectedServiceName) }

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
                Text(text = specialist.name, style = RojanTypography.Body, color = ManagerColors.TextPrimary)
                if (specialist.skills.isNotEmpty()) {
                    Text(
                        text = specialist.skills.joinToString(" · "),
                        style = RojanTypography.Caption,
                        color = ManagerColors.TextSecondary,
                    )
                }
                if (specialist.workingHours.isNotBlank()) {
                    Text(
                        text = specialist.workingHours,
                        style = RojanTypography.Caption,
                        color = ManagerColors.Gold,
                    )
                }
            }
        }
    }
}
