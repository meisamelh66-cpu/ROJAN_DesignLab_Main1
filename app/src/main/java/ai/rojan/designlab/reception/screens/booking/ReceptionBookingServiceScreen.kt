package ai.rojan.designlab.reception.screens.booking

import ai.rojan.designlab.domain.repository.Service
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
fun ReceptionBookingServiceScreen(
    viewModel: ReceptionBookingViewModel,
    onBackClick: () -> Unit,
    onServiceSelected: () -> Unit,
) {
    val services by viewModel.services.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadServices() }

    ReceptionScaffold(onBackClick = onBackClick) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD)) {
            Text(text = "انتخاب خدمت", style = RojanTypography.ScreenTitle, color = ReceptionPalette.textPrimary)

            ReceptionUiStateList(
                state = services,
                emptyMessage = "خدمتی برای این سالن ثبت نشده است",
                modifier = Modifier.fillMaxSize(),
                onRetryClick = viewModel::loadServices,
            ) { service ->
                ServiceRow(service = service, onClick = {
                    viewModel.selectService(service)
                    onServiceSelected()
                })
            }
        }
    }
}

@Composable
private fun ServiceRow(service: Service, onClick: () -> Unit) {
    ReceptionGlassSurface(
        modifier = Modifier.fillMaxWidth().rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD)) {
            Text(text = service.name, style = RojanTypography.CardTitle, color = ReceptionPalette.textPrimary)
            Text(
                text = "${service.durationMinutes} دقیقه",
                style = RojanTypography.Caption,
                color = ReceptionPalette.textSecondary,
                modifier = Modifier.padding(top = RojanDimens.SpaceXS),
            )
        }
    }
}
