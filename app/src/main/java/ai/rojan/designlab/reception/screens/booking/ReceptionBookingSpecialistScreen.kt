package ai.rojan.designlab.reception.screens.booking

import ai.rojan.designlab.domain.repository.Specialist
import ai.rojan.designlab.reception.components.ReceptionGlassSurface
import ai.rojan.designlab.reception.components.ReceptionScaffold
import ai.rojan.designlab.reception.components.ReceptionUiStateList
import ai.rojan.designlab.reception.presentation.booking.ReceptionBookingViewModel
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.ReceptionPalette
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ReceptionBookingSpecialistScreen(
    viewModel: ReceptionBookingViewModel,
    onBackClick: () -> Unit,
    onSpecialistSelected: () -> Unit,
) {
    val specialists by viewModel.specialists.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadSpecialists() }

    ReceptionScaffold(onBackClick = onBackClick) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD)) {
            Text(text = "انتخاب متخصص", style = RojanTypography.ScreenTitle, color = ReceptionPalette.textPrimary)

            ReceptionUiStateList(
                state = specialists,
                emptyMessage = "متخصصی برای این سالن ثبت نشده است",
                modifier = Modifier.fillMaxSize(),
            ) { specialist ->
                SpecialistRow(specialist = specialist, onClick = {
                    viewModel.selectSpecialist(specialist)
                    onSpecialistSelected()
                })
            }
        }
    }
}

@Composable
private fun SpecialistRow(specialist: Specialist, onClick: () -> Unit) {
    ReceptionGlassSurface(
        modifier = Modifier.fillMaxWidth().rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
    ) {
        Text(
            text = specialist.displayName,
            style = RojanTypography.CardTitle,
            color = ReceptionPalette.textPrimary,
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
        )
    }
}
