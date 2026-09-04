package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.usecase.relationship.GetFavoriteSalonsUseCase
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.relationship.FavoriteSalonsViewModel
import ai.rojan.designlab.presentation.relationship.FavoriteSalonsViewModelFactory
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.rtl.RtlListRow
import ai.rojan.designlab.ui.components.state.RojanEmptyState
import ai.rojan.designlab.ui.components.state.RojanErrorState
import ai.rojan.designlab.ui.components.state.RojanLoadingState
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Journey 2, Screen 3: Favorites.
 *
 * Customer Relationship Foundation, Phase 5/6: real data via
 * [FavoriteSalonsViewModel] -> `GET /api/v1/customer/favorite-salons`
 * (self-scoped by JWT). Replaces the `RojanComingSoonState` this screen
 * showed during Production Data Integrity Phase 1, when no favorite/follow
 * endpoint existed on the backend yet.
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
    HomeBackgroundTheme {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item { GlassBackButton(onClick = onBackClick) }
            item { Text("علاقه‌مندی‌ها", style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary) }

            when (val loadState = viewModel.state) {
                is UiState.Loading -> item { RojanLoadingState(message = "در حال بارگذاری...") }
                is UiState.Empty -> item {
                    RojanEmptyState(
                        title = "هنوز سالنی را ذخیره نکرده‌اید",
                        description = "سالن‌های مورد علاقه خود را برای دسترسی سریع‌تر ذخیره کنید.",
                        icon = Icons.Filled.FavoriteBorder,
                    )
                }
                is UiState.Error -> item {
                    RojanErrorState(description = loadState.message, actionLabel = "تلاش مجدد", onAction = viewModel::retry)
                }
                is UiState.Success -> {
                    itemsIndexed(loadState.data, key = { _, item -> item.salonId }) { index, item ->
                        HomeGlassSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .rojanEnterAnimation(delayMillis = index * 60)
                                .rojanPressable(onClick = { onSalonClick(item.salonId) }),
                            shape = RojanShapes.Small,
                        ) {
                            RtlListRow(
                                title = item.salonName ?: "سالن",
                                titleColor = HomeColors.TextPrimary,
                                subtitle = item.salonAddress,
                                subtitleColor = HomeColors.TextSecondary,
                                icon = Icons.Filled.Favorite,
                                iconTint = HomeColors.Glow,
                                modifier = Modifier.padding(RojanDimens.SpaceMD),
                            )
                        }
                    }
                }
            }
        }
    }
}
