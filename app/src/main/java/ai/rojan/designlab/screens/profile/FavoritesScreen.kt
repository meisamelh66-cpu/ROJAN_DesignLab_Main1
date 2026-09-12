package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.usecase.relationship.GetFavoriteSalonsUseCase
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.relationship.FavoriteSalonsViewModel
import ai.rojan.designlab.presentation.relationship.FavoriteSalonsViewModelFactory
import ai.rojan.designlab.screens.customer.components.CustomerAccent
import ai.rojan.designlab.screens.customer.components.CustomerEmptyState
import ai.rojan.designlab.screens.customer.components.CustomerErrorState
import ai.rojan.designlab.screens.customer.components.CustomerLoadingState
import ai.rojan.designlab.screens.customer.components.CustomerScaffold
import ai.rojan.designlab.screens.customer.components.CustomerScreenMargin
import ai.rojan.designlab.screens.customer.components.RefListRow
import ai.rojan.designlab.screens.customer.components.RefSurface
import ai.rojan.designlab.ui.theme.RojanDimens

/**
 * Journey 2, Screen 3: Favorites.
 *
 * Real data via [FavoriteSalonsViewModel] -> `GET /api/v1/customer/favorite-salons`
 * (self-scoped by JWT).
 *
 * Quiet Luxury pass (visual only). The `GlassBackButton` orb, the bare
 * `HeroTitle`, the `HomeGlassSurface` cards + `RtlListRow` with the violet
 * `HomeColors.Glow` heart, the per-item `rojanEnterAnimation` stagger, and
 * the glass `RojanLoadingState` / `RojanEmptyState` / `RojanErrorState` are
 * replaced with the [CustomerScaffold] shell and the flat foundation
 * primitives: [RefSurface] + [RefListRow] rows with a rose-gold outlined
 * heart, and [CustomerLoadingState] / [CustomerEmptyState] /
 * [CustomerErrorState].
 *
 * NOTHING about behaviour changed: `retry()` and every `on*` callback
 * ([onBackClick], [onSalonClick]) are called exactly where they were. No
 * ViewModel, repository, API, or navigation route is touched.
 */
@Composable
fun FavoritesScreen(
    onBackClick: () -> Unit,
    onSalonClick: (String) -> Unit,
    viewModel: FavoriteSalonsViewModel = viewModel(
        factory = run {
            val container = BackendApiContainerHolder.get(LocalContext.current)
            FavoriteSalonsViewModelFactory(
                getFavoriteSalonsUseCase = GetFavoriteSalonsUseCase(container.customerRelationshipRepository),
                salonRepository = container.salonRepository,
            )
        },
    ),
) {
    CustomerScaffold(title = "علاقه‌مندی‌ها", onBackClick = onBackClick) {
        when (val loadState = viewModel.state) {
            is UiState.Loading -> CustomerLoadingState(
                modifier = Modifier.padding(top = RojanDimens.SpaceLG),
                count = 5,
                rowHeight = 72,
            )

            is UiState.Empty -> CustomerEmptyState(
                title = "هنوز سالنی را ذخیره نکرده‌اید",
                body = "سالن‌های مورد علاقه خود را برای دسترسی سریع‌تر ذخیره کنید.",
                icon = Icons.Outlined.FavoriteBorder,
            )

            is UiState.Error -> CustomerErrorState(
                message = loadState.message,
                onRetry = viewModel::retry,
            )

            is UiState.Success -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = RojanDimens.SpaceLG),
                verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
            ) {
                items(loadState.data, key = { it.salonId }) { item ->
                    RefSurface(modifier = Modifier.padding(horizontal = CustomerScreenMargin)) {
                        RefListRow(
                            title = item.salonName ?: "سالن",
                            subtitle = item.salonAddress?.takeIf { it.isNotBlank() },
                            onClick = { onSalonClick(item.salonId) },
                            leading = {
                                Icon(
                                    Icons.Outlined.FavoriteBorder,
                                    contentDescription = null,
                                    tint = CustomerAccent,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
