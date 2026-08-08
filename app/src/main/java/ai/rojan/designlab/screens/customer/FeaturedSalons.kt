package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Storefront
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.salon.SalonListViewModel
import ai.rojan.designlab.presentation.salon.SalonListViewModelFactory
import ai.rojan.designlab.screens.customer.hometheme.HomeCard
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.state.RojanLoadingState
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.theme.salonAccentColorFor

/**
 * Customer Home featured salons — Design Board v1.0, Secondary Features
 * layer. Purpose is trust-building and inspiration, explicitly not the
 * main booking action — visual priority sits below HeroBookingCard and
 * AISearchBar, above Recommendations/History sections, per the Board.
 *
 * Production Data Integrity Phase 1: now backed by the real
 * `GET /api/v1/salons` (via the same [SalonListViewModel] `SalonListScreen`/
 * `SearchScreen` already use), not [ai.rojan.designlab.domain.catalog.CatalogEngine].
 * Same real-data gaps as those two screens, not papered over: the backend
 * has no rating or per-salon photo concept, so this card no longer shows
 * either — [ai.rojan.designlab.ui.theme.salonAccentColorFor] (a
 * deterministic, decorative-only tint) replaces `DemoSalon.colorSeed`, and
 * the real `address` field replaces the demo `tagline` under the location
 * icon.
 */
@Composable
fun FeaturedSalons(
    onSalonClick: (String) -> Unit = {},
    viewModel: SalonListViewModel = viewModel(
        factory = SalonListViewModelFactory(
            BackendApiContainerHolder.get(LocalContext.current).salonRepository,
        ),
    ),
) {
    val salons = (viewModel.state as? UiState.Success)?.data.orEmpty()

    if (viewModel.state is UiState.Loading) {
        RojanLoadingState()
        return
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        itemsIndexed(salons) { index, salon ->
            HomeCard(
                accentColor = salonAccentColorFor(salon.id),
                // Visual Refinement: tint reduced 0.30f -> 0.25f (~17%). No
                // literal "dark overlay on the salon photo" exists in this
                // component (the Image draws opaque, fully covering this
                // background) - this is the closest real, honest mapping of
                // "reduce overlay / clearer image" to something that
                // actually affects this card's appearance.
                accentAlpha = 0.25f,
                onClick = { onSalonClick(salon.id) },
                index = index,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(salonAccentColorFor(salon.id).copy(alpha = 0.5f), RojanShapes.Small),
                    contentAlignment = Alignment.Center,
                ) {
                    RojanIconContainer(
                        imageVector = Icons.Filled.Storefront,
                        contentDescription = null,
                        tint = HomeColors.TextPrimary,
                        size = RojanIconSize.Large,
                    )
                }

                Text(
                    text = salon.name,
                    style = RojanTypography.Caption,
                    color = HomeColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                ) {
                    RojanIconContainer(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = HomeColors.TextSecondary,
                        size = RojanIconSize.Small,
                    )
                    Text(
                        text = salon.address,
                        style = RojanTypography.Caption,
                        color = HomeColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
