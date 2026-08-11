package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.usecase.relationship.GetFollowedSalonsUseCase
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.relationship.FollowedSalonsViewModel
import ai.rojan.designlab.presentation.relationship.FollowedSalonsViewModelFactory
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
 * Customer Relationship Foundation, Phase 5/6: the customer's own actively
 * followed salons, real data via [FollowedSalonsViewModel] ->
 * `GET /api/v1/customer/followed-salons` (self-scoped by JWT - no
 * customerId anywhere on this path, so no cross-customer data to leak).
 */
@Composable
fun FollowedSalonsScreen(
    onBackClick: () -> Unit,
    onSalonClick: (String) -> Unit,
    viewModel: FollowedSalonsViewModel = viewModel(
        factory = run {
            val container = BackendApiContainerHolder.get(LocalContext.current)
            FollowedSalonsViewModelFactory(
                getFollowedSalonsUseCase = GetFollowedSalonsUseCase(container.customerRelationshipRepository),
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
            item { Text("سالن‌های دنبال‌شده", style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary) }

            when (val loadState = viewModel.state) {
                is UiState.Loading -> item { RojanLoadingState(message = "در حال بارگذاری...") }
                is UiState.Empty -> item {
                    RojanEmptyState(
                        title = "هنوز سالنی را دنبال نکرده‌اید",
                        description = "برای دریافت اخبار و به‌روزرسانی‌های یک سالن، آن را دنبال کنید.",
                        icon = Icons.Filled.NotificationsNone,
                    )
                }
                is UiState.Error -> item {
                    RojanErrorState(description = loadState.message, actionLabel = "تلاش مجدد", onAction = viewModel::retry)
                }
                is UiState.Success -> {
                    itemsIndexed(loadState.data) { index, item ->
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
                                icon = Icons.Filled.NotificationsNone,
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
