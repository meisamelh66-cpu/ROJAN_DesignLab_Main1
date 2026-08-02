package ai.rojan.designlab.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.repository.Specialist
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.specialist.SpecialistSelectionViewModel
import ai.rojan.designlab.presentation.specialist.SpecialistSelectionViewModelFactory
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.image.SpecialistAvatar
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.state.RojanEmptyState
import ai.rojan.designlab.ui.components.state.RojanErrorState
import ai.rojan.designlab.ui.components.state.RojanLoadingState
import ai.rojan.designlab.ui.theme.RojanAquaMint
import ai.rojan.designlab.ui.theme.RojanBlushPink
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanPearlPink
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanSoftLavender
import ai.rojan.designlab.ui.theme.RojanTypography

/** Same deterministic-tint reasoning as [ai.rojan.designlab.screens.salon.SalonDetailsScreen]'s `accentFor`. */
private val accentPalette = listOf(RojanSoftLavender, RojanAquaMint, RojanBlushPink, RojanPearlPink)
private fun accentFor(id: String) = accentPalette[Math.floorMod(id.hashCode(), accentPalette.size)]

/**
 * Booking Experience Refactor, spec section 11 — Specialist Selection.
 *
 * **Android <-> Backend Full Integration milestone:** now backed by
 * [SpecialistSelectionViewModel] -> `GET /api/v1/salons/{salonId}/specialists`.
 * The former "earliest available appointment, then rating" priority order
 * is gone — see [SpecialistSelectionViewModel]'s doc comment: it needed
 * per-specialist availability data (this milestone's Phase 5, not this
 * screen) and a rating field the backend doesn't have. Specialists list in
 * whatever order the backend returns, a disclosed simplification rather
 * than a fabricated ordering. [durationMinutes], only ever used to compute
 * that ordering, is removed along with it.
 *
 * The "skip this page" half of the parent rule is still enforced one level
 * up, in Navigation (this screen is simply never navigated to when
 * [ai.rojan.designlab.screens.salon.SalonDetailsScreen] already found
 * exactly one specialist).
 */
@Composable
fun SpecialistSelectionScreen(
    salonId: String,
    onBackClick: () -> Unit,
    onSpecialistSelected: (String) -> Unit,
    viewModel: SpecialistSelectionViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = SpecialistSelectionViewModelFactory(
            salonId = salonId,
            specialistRepository = BackendApiContainerHolder.get(LocalContext.current).specialistRepository,
        ),
    ),
) {
    HomeBackgroundTheme {
        Column(modifier = Modifier.fillMaxSize().padding(RojanDimens.SpaceMD)) {
            GlassBackButton(onClick = onBackClick)

            Text(
                text = "انتخاب متخصص",
                style = RojanTypography.HeroTitle,
                color = HomeColors.TextPrimary,
                modifier = Modifier.padding(vertical = RojanDimens.SpaceMD),
            )

            when (val state = viewModel.state) {
                is UiState.Loading -> RojanLoadingState(message = "در حال بارگذاری متخصصان...")
                is UiState.Error -> RojanErrorState(
                    description = state.message,
                    actionLabel = "تلاش مجدد",
                    onAction = { viewModel.retry() },
                )
                is UiState.Empty -> RojanEmptyState(title = "متخصصی برای این سالن یافت نشد")
                is UiState.Success -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                        itemsIndexed(state.data) { index, specialist ->
                            SpecialistRow(
                                specialist = specialist,
                                onClick = { onSpecialistSelected(specialist.id) },
                                animationDelayMillis = index * 60,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecialistRow(specialist: Specialist, onClick: () -> Unit, animationDelayMillis: Int = 0) {
    HomeGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .rojanEnterAnimation(delayMillis = animationDelayMillis)
            .rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            Box(
                modifier = Modifier.size(56.dp).background(accentFor(specialist.id).copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                SpecialistAvatar(
                    assetRes = null,
                    contentDescription = specialist.displayName,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column {
                Text(specialist.displayName, style = RojanTypography.Body, color = HomeColors.TextPrimary)
                specialist.bio?.let { bio ->
                    Text(bio, style = RojanTypography.Caption, color = HomeColors.TextSecondary)
                }
            }
        }
    }
}
