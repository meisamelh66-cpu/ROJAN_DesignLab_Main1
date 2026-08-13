package ai.rojan.designlab.reception.screens.auth

import ai.rojan.designlab.domain.repository.AvailableSalon
import ai.rojan.designlab.domain.repository.SalonAccessType
import ai.rojan.designlab.reception.components.ReceptionGlassSurface
import ai.rojan.designlab.reception.components.ReceptionScaffold
import ai.rojan.designlab.reception.domain.auth.ActiveSalonUiState
import ai.rojan.designlab.reception.presentation.auth.ReceptionAuthViewModel
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.ReceptionPalette
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
 * Shown only when [ReceptionAuthViewModel.activeSalonState] is
 * [ActiveSalonUiState.SelectionRequired] — same reasoning as
 * [ai.rojan.designlab.manager.screens.auth.ManagerSalonSelectionScreen].
 * Navigation-agnostic: never calls a NavController itself, only
 * [onSalonSelected].
 *
 * See ROJAN_Reception_Implementation_Plan_v1.md, Phase 0.
 */
@Composable
fun ReceptionSalonSelectionScreen(
    viewModel: ReceptionAuthViewModel,
    onSalonSelected: () -> Unit,
) {
    val activeSalonState by viewModel.activeSalonState.collectAsStateWithLifecycle()

    LaunchedEffect(activeSalonState) {
        if (activeSalonState is ActiveSalonUiState.Active) onSalonSelected()
    }

    ReceptionScaffold {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item {
                RtlSectionHeader(
                    text = "انتخاب سالن",
                    style = RojanTypography.ScreenTitle,
                    color = ReceptionPalette.textPrimary,
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
                    // Phase 1 (authentication completion): a retry
                    // affordance for this branch — previously this notice
                    // was a dead end with no way forward except leaving the
                    // screen entirely.
                    item {
                        SalonSelectionNotice(
                            text = state.message,
                            onRetryClick = viewModel::retryIdentityResolution,
                        )
                    }
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
    ReceptionGlassSurface(
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
                Text(text = salon.salonName, style = RojanTypography.CardTitle, color = ReceptionPalette.textPrimary)
            }
            AccessTypeChip(text = salon.accessType.displayLabel)
        }
    }
}

/** Minimal local access-type badge — same "no shared chip component to reuse yet" precedent as the OTP screen's own `ReceptionTextField`; deliberately not importing Manager's `TagChip` to keep the two flavors independent (see `ReceptionAuthState.kt`'s own doc comment). */
@Composable
private fun AccessTypeChip(text: String) {
    ReceptionGlassSurface(shape = RojanShapes.Small) {
        Text(
            text = text,
            style = RojanTypography.Caption,
            color = ReceptionPalette.textAccent,
            modifier = Modifier.padding(horizontal = RojanDimens.SpaceSM, vertical = RojanDimens.SpaceXS),
        )
    }
}

private val SalonAccessType.displayLabel: String
    get() = when (this) {
        SalonAccessType.OWNER -> "مالک"
        SalonAccessType.MEMBER -> "عضو"
        SalonAccessType.SPECIALIST -> "متخصص"
    }

@Composable
private fun SalonSelectionNotice(text: String, onRetryClick: (() -> Unit)? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ReceptionGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RojanShapes.Small,
        ) {
            Text(
                text = text,
                style = RojanTypography.Body,
                color = ReceptionPalette.textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RojanDimens.SpaceLG),
                textAlign = TextAlign.Center,
            )
        }

        if (onRetryClick != null) {
            Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))
            PremiumButton(
                text = "تلاش مجدد",
                onClick = onRetryClick,
            )
        }
    }
}
