package ai.rojan.designlab.manager.screens.auth

import ai.rojan.designlab.domain.repository.AvailableSalon
import ai.rojan.designlab.domain.repository.SalonAccessType
import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.domain.auth.ActiveSalonUiState
import ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModel
import ai.rojan.designlab.manager.screens.customers.TagChip
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Active Salon Context & Selection Flow — shown only when
 * [ManagerAuthViewModel.activeSalonState] is [ActiveSalonUiState.SelectionRequired]
 * (more than one salon available for this account, none already validly
 * selected). Navigation-agnostic like [ManagerOtpAuthScreen]: never calls a
 * NavController itself, only [onSalonSelected], invoked once via a
 * [LaunchedEffect] watching [ManagerAuthViewModel.activeSalonState] turning
 * [ActiveSalonUiState.Active] after [ManagerAuthViewModel.selectSalon].
 * Card styling mirrors [ai.rojan.designlab.manager.screens.customers.ManagerCustomersListScreen]'s
 * `CustomerCard`/[TagChip] pattern.
 */
@Composable
fun ManagerSalonSelectionScreen(
    viewModel: ManagerAuthViewModel,
    onSalonSelected: () -> Unit,
) {
    val activeSalonState by viewModel.activeSalonState.collectAsStateWithLifecycle()

    LaunchedEffect(activeSalonState) {
        if (activeSalonState is ActiveSalonUiState.Active) onSalonSelected()
    }

    ManagerScaffold {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item {
                RtlSectionHeader(
                    text = "انتخاب سالن",
                    style = RojanTypography.ScreenTitle,
                    color = ManagerColors.TextPrimary,
                    horizontalPadding = 0.dp,
                )
            }

            when (val state = activeSalonState) {
                is ActiveSalonUiState.SelectionRequired -> {
                    items(state.options) { option ->
                        SalonOptionCard(
                            salon = option,
                            onClick = { viewModel.selectSalon(option) },
                        )
                    }
                }
                is ActiveSalonUiState.Error -> {
                    item { SalonSelectionNotice(text = state.message) }
                }
                ActiveSalonUiState.Loading, is ActiveSalonUiState.Active -> {
                    item { SalonSelectionNotice(text = "در حال بارگذاری…") }
                }
            }
        }
    }
}

@Composable
private fun SalonOptionCard(salon: AvailableSalon, onClick: () -> Unit) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(text = salon.salonName, style = RojanTypography.CardTitle, color = ManagerColors.TextPrimary)
            }
            TagChip(text = salon.accessType.displayLabel)
        }
    }
}

private val SalonAccessType.displayLabel: String
    get() = when (this) {
        SalonAccessType.OWNER -> "مالک"
        SalonAccessType.MEMBER -> "عضو"
        SalonAccessType.SPECIALIST -> "متخصص"
    }

@Composable
private fun SalonSelectionNotice(text: String) {
    ManagerGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RojanShapes.Small,
    ) {
        Text(
            text = text,
            style = RojanTypography.Body,
            color = ManagerColors.TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceLG),
            textAlign = TextAlign.Center,
        )
    }
}
