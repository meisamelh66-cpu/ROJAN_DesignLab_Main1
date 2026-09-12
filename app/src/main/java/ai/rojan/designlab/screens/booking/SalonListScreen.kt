package ai.rojan.designlab.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.salon.SalonListViewModel
import ai.rojan.designlab.screens.customer.components.CustomerAccent
import ai.rojan.designlab.screens.customer.components.CustomerCardShape
import ai.rojan.designlab.screens.customer.components.CustomerEmptyState
import ai.rojan.designlab.screens.customer.components.CustomerErrorState
import ai.rojan.designlab.screens.customer.components.CustomerHairline
import ai.rojan.designlab.screens.customer.components.CustomerLoadingState
import ai.rojan.designlab.screens.customer.components.CustomerOnAccent
import ai.rojan.designlab.screens.customer.components.CustomerScaffold
import ai.rojan.designlab.screens.customer.components.CustomerScreenMargin
import ai.rojan.designlab.screens.customer.components.CustomerSearchField
import ai.rojan.designlab.screens.customer.components.CustomerSurfaceFill
import ai.rojan.designlab.screens.customer.components.RefSurface
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.components.image.RojanRemoteImage
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

private const val SEARCH_DEBOUNCE_MS = 350L

/**
 * Booking Journey — Salon picker.
 *
 * Quiet Luxury pass (visual only). The `GlassBackButton` orb, the bare
 * `HeroTitle`, the violet-glow `HomeTextField` search bar, the
 * `HomeGlassSurface` filter chips + "ورود کسب‌وکار" pill, the
 * `PremiumCardShell` / `HomeGlassSurface` salon cards + skeletons, the
 * violet `HomeColors.Glow` follow/favourite tints and spinner, the
 * `salonAccentColorFor` tile tints, the per-item `rojanEnterAnimation`
 * stagger, and the glass `RojanEmptyState` / `RojanErrorState` are replaced
 * with the [CustomerScaffold] shell and the flat foundation primitives:
 * a rose-gold-cursor search field, quiet filter pills, [RefSurface] cards,
 * outlined icons, and [CustomerLoadingState] / [CustomerEmptyState] /
 * [CustomerErrorState].
 *
 * NOTHING about behaviour changed: debounced backend search, pagination on
 * scroll, the `LifecycleResumeEffect` stale-401 retry, `showBackButton`
 * (still hides the back affordance on the Home-rooted entry), the
 * `onBusinessLoginClick` guard, and every `on*` callback are called exactly
 * where they were. `selectedServiceIds` still browses all active salons
 * (no backend cross-salon "offers all of these" lookup exists — same
 * disclosed gap as before). No ViewModel, repository, API, or navigation
 * route is touched.
 */
@Composable
fun SalonListScreen(
    selectedServiceIds: List<String>,
    onBackClick: () -> Unit,
    onSalonSelected: (String) -> Unit,
    showBackButton: Boolean = true,
    onBusinessLoginClick: (() -> Unit)? = null,
    onLoginRequired: (() -> Unit)? = null,
    viewModel: SalonListViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = run {
            val container = ai.rojan.designlab.di.BackendApiContainerHolder.get(androidx.compose.ui.platform.LocalContext.current)
            ai.rojan.designlab.presentation.salon.SalonListViewModelFactory(
                salonRepository = container.salonRepository,
                getFollowedSalonsUseCase = ai.rojan.designlab.domain.usecase.relationship.GetFollowedSalonsUseCase(container.customerRelationshipRepository),
                getFavoriteSalonsUseCase = ai.rojan.designlab.domain.usecase.relationship.GetFavoriteSalonsUseCase(container.customerRelationshipRepository),
                publicSalonRepository = container.publicSalonRepository,
                hasSession = { container.tokenRepository.accessToken()?.isNotBlank() == true },
            )
        },
    ),
) {
    // Protected Route Handling fix: an anonymous customer redirected to AUTH
    // from here returns to this exact NavBackStackEntry - same ViewModel
    // instance, still holding its stale pre-login 401 error. Retrying on every
    // resume (guarded by isUnauthorized, so it's a no-op on the ordinary
    // first-launch resume) picks the real data back up automatically.
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        if (viewModel.isUnauthorized) {
            viewModel.retry()
        }
        onPauseOrDispose { }
    }

    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(SalonSortOption.ALL) }
    var isFirstComposition by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()

    LaunchedEffect(searchQuery) {
        if (isFirstComposition) {
            isFirstComposition = false
        } else {
            delay(SEARCH_DEBOUNCE_MS)
            viewModel.load(searchQuery.takeIf { it.isNotBlank() })
            // T2-1: a new search swaps in a fresh page-0 result set; scroll back
            // to the top so the new results don't render from the old offset.
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(listState, viewModel.canLoadMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val total = (viewModel.state as? UiState.Success)?.data?.size ?: return@collect
                if (lastVisibleIndex != null && lastVisibleIndex >= total - 4) {
                    viewModel.loadMore()
                }
            }
    }

    CustomerScaffold(
        title = "انتخاب سالن",
        onBackClick = onBackClick,
        showBackButton = showBackButton,
    ) {
        if (onBusinessLoginClick != null) {
            Text(
                "ورود کسب‌وکار",
                style = RojanTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                color = CustomerAccent,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = CustomerScreenMargin)
                    .padding(top = RojanDimens.SpaceSM)
                    .rojanPressable(onClick = onBusinessLoginClick, role = Role.Button)
                    .padding(RojanDimens.SpaceXS),
            )
        }

        SearchField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .padding(horizontal = CustomerScreenMargin)
                .padding(top = RojanDimens.SpaceMD),
        )

        Spacer(Modifier.height(RojanDimens.SpaceMD))

        Row(
            modifier = Modifier.padding(horizontal = CustomerScreenMargin),
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            FilterPill("همه", sortOption == SalonSortOption.ALL) { sortOption = SalonSortOption.ALL }
            FilterPill("نزدیک من", sortOption == SalonSortOption.NEAREST) { sortOption = SalonSortOption.NEAREST }
        }

        Spacer(Modifier.height(RojanDimens.SpaceMD))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
          when (val state = viewModel.state) {
            is UiState.Loading -> CustomerLoadingState(count = 5, rowHeight = 88)

            is UiState.Error -> if (viewModel.isUnauthorized && onLoginRequired != null) {
                CustomerErrorState(
                    message = state.message,
                    title = "برای مشاهده سالن‌ها وارد شوید",
                    retryLabel = "ورود",
                    onRetry = onLoginRequired,
                )
            } else {
                CustomerErrorState(
                    message = state.message,
                    onRetry = { viewModel.retry() },
                )
            }

            is UiState.Empty -> CustomerEmptyState(
                title = if (searchQuery.isBlank()) "سالنی یافت نشد" else "سالنی با این جستجو یافت نشد",
                icon = Icons.Outlined.Storefront,
            )

            is UiState.Success -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = RojanDimens.SpaceLG),
                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                ) {
                    itemsIndexed(state.data, key = { _, salon -> salon.id }) { _, salon ->
                        SalonCard(
                            salon = salon,
                            isFollowing = viewModel.followedSalonIds.contains(salon.id),
                            isFavorite = viewModel.favoriteSalonIds.contains(salon.id),
                            onClick = { onSalonSelected(salon.id) },
                            modifier = Modifier.padding(horizontal = CustomerScreenMargin),
                        )
                    }
                    if (viewModel.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = CustomerAccent,
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                }
            }
          }
        }
    }
}

private enum class SalonSortOption { ALL, NEAREST }

// --- Search field (rose-gold cursor, flat surface) -------------------------

// Phase 4 (P1): shared with SearchScreen as `CustomerSearchField` — same flat
// visual, RTL, focus and keyboard behaviour, just no longer copied.
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) = CustomerSearchField(
    value = value,
    onValueChange = onValueChange,
    placeholder = "جستجوی سالن...",
    modifier = modifier,
)

// --- Quiet filter pill ----------------------------------------------------

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) CustomerAccent else CustomerSurfaceFill)
            .border(1.dp, if (selected) CustomerAccent else CustomerHairline, RoundedCornerShape(999.dp))
            .rojanPressable(onClick = onClick, role = Role.Button)
            .heightIn(min = 36.dp)
            .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceSM),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = RojanTypography.Caption,
            color = if (selected) CustomerOnAccent else HomeColors.TextSecondary,
        )
    }
}

// --- Salon card ---------------------------------------------------------

@Composable
private fun SalonCard(
    salon: Salon,
    isFollowing: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RefSurface(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .rojanPressable(onClick = onClick, role = Role.Button)
                .padding(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = null,
                tint = HomeColors.TextMuted,
                modifier = Modifier.size(20.dp),
            )

            if (isFollowing || isFavorite) {
                Spacer(Modifier.width(RojanDimens.SpaceSM))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                ) {
                    if (isFollowing) {
                        Icon(
                            Icons.Outlined.NotificationsActive,
                            contentDescription = "دنبال‌شده",
                            tint = CustomerAccent,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    if (isFavorite) {
                        Icon(
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = "مورد علاقه",
                            tint = CustomerAccent,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    salon.name,
                    style = RojanTypography.Body,
                    color = HomeColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                salon.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Spacer(Modifier.height(RojanDimens.SpaceXS))
                    Text(
                        description,
                        style = RojanTypography.Caption,
                        color = HomeColors.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.width(RojanDimens.SpaceMD))

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CustomerCardShape)
                    .background(CustomerSurfaceFill),
                contentAlignment = Alignment.Center,
            ) {
                RojanRemoteImage(
                    url = salon.logoUrl,
                    contentDescription = salon.name,
                    shape = CustomerCardShape,
                    modifier = Modifier.fillMaxSize(),
                    fallback = {
                        Icon(
                            Icons.Outlined.Storefront,
                            contentDescription = null,
                            tint = HomeColors.TextMuted,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                )
            }
        }
    }
}
