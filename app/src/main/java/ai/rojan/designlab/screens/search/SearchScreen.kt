package ai.rojan.designlab.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.domain.usecase.relationship.GetFavoriteSalonsUseCase
import ai.rojan.designlab.domain.usecase.relationship.GetFollowedSalonsUseCase
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.salon.SalonListViewModel
import ai.rojan.designlab.presentation.salon.SalonListViewModelFactory
import ai.rojan.designlab.screens.customer.components.CustomerAccent
import ai.rojan.designlab.screens.customer.components.CustomerCardShape
import ai.rojan.designlab.screens.customer.components.CustomerEmptyState
import ai.rojan.designlab.screens.customer.components.CustomerErrorState
import ai.rojan.designlab.screens.customer.components.CustomerLoadingState
import ai.rojan.designlab.screens.customer.components.CustomerScaffold
import ai.rojan.designlab.screens.customer.components.CustomerScreenMargin
import ai.rojan.designlab.screens.customer.components.CustomerSearchField
import ai.rojan.designlab.screens.customer.components.CustomerSurfaceFill
import ai.rojan.designlab.screens.customer.components.RefSurface
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.components.image.RojanRemoteImage
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

private const val SEARCH_DEBOUNCE_MS = 350L

/**
 * Journey 1, Screen 1: Search.
 *
 * Quiet Luxury pass (visual only). The `GlassBackButton` orb, the bare
 * `HeroTitle`, the `HomeGlassSurface` search bar + result cards + skeletons,
 * the violet `HomeColors.Glow` search icon / cursor / follow-favourite tints,
 * the per-salon `RojanSoftLavender`/…-family tile tints, the per-item
 * `rojanEnterAnimation` stagger, and the glass `RojanEmptyState` /
 * `RojanErrorState` are replaced with the [CustomerScaffold] shell and the
 * flat foundation primitives: a rose-gold-cursor search field, [RefSurface]
 * result rows, outlined icons, and [CustomerLoadingState] /
 * [CustomerEmptyState] / [CustomerErrorState].
 *
 * NOTHING about behaviour changed: debounced backend search
 * (`GET /api/v1/salons?name=` / `GET /api/v1/public/salons?search=` for a
 * guest), pagination on scroll, the `LifecycleResumeEffect` stale-401 retry,
 * and every `on*` callback are called exactly where they were. No ViewModel,
 * repository, API, or navigation route is touched.
 */
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onSalonClick: (String) -> Unit,
    onLoginRequired: (() -> Unit)? = null,
    viewModel: SalonListViewModel = viewModel(
        factory = run {
            val container = BackendApiContainerHolder.get(LocalContext.current)
            SalonListViewModelFactory(
                salonRepository = container.salonRepository,
                getFollowedSalonsUseCase = GetFollowedSalonsUseCase(container.customerRelationshipRepository),
                getFavoriteSalonsUseCase = GetFavoriteSalonsUseCase(container.customerRelationshipRepository),
                publicSalonRepository = container.publicSalonRepository,
                hasSession = { container.tokenRepository.accessToken()?.isNotBlank() == true },
            )
        },
    ),
) {
    // Protected Route Handling fix: see SalonListScreen's identical LifecycleResumeEffect for why this is needed - same shared SalonListViewModel, same stale-401-after-login gap.
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        if (viewModel.isUnauthorized) {
            viewModel.retry()
        }
        onPauseOrDispose { }
    }

    var query by remember { mutableStateOf("") }
    var isFirstComposition by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()

    LaunchedEffect(query) {
        if (isFirstComposition) {
            isFirstComposition = false
        } else {
            delay(SEARCH_DEBOUNCE_MS)
            viewModel.load(query.takeIf { it.isNotBlank() })
            // T2-1: a new search swaps in a fresh page-0 result set while the
            // previous list can stay mounted (SalonListViewModel.load's
            // `isSearching` path), so without this the new results would render
            // from the old scroll offset. loadMore() never changes `query`, so
            // pagination keeps the user's position.
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

    CustomerScaffold(title = "جستجو", onBackClick = onBackClick) {
        SearchField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .padding(horizontal = CustomerScreenMargin)
                .padding(top = RojanDimens.SpaceMD),
        )

        Spacer(Modifier.height(RojanDimens.SpaceMD))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val state = viewModel.state) {
                is UiState.Loading -> CustomerLoadingState(count = 6, rowHeight = 76)

                is UiState.Error -> if (viewModel.isUnauthorized && onLoginRequired != null) {
                    CustomerErrorState(
                        message = state.message,
                        title = "برای جستجوی سالن‌ها وارد شوید",
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
                    title = "نتیجه‌ای یافت نشد",
                    body = "نام سالن دیگری را جستجو کنید",
                    icon = Icons.Outlined.SearchOff,
                )

                is UiState.Success -> Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        "نتایج (${state.data.size})",
                        style = RojanTypography.Caption,
                        color = HomeColors.TextMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = CustomerScreenMargin),
                    )
                    Spacer(Modifier.height(RojanDimens.SpaceSM))
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = RojanDimens.SpaceLG),
                        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                    ) {
                        itemsIndexed(state.data, key = { _, salon -> salon.id }) { _, salon ->
                            SearchResultRow(
                                salon = salon,
                                isFollowing = viewModel.followedSalonIds.contains(salon.id),
                                isFavorite = viewModel.favoriteSalonIds.contains(salon.id),
                                onClick = { onSalonClick(salon.id) },
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
                                        modifier = Modifier.size(20.dp),
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

// --- Search field (rose-gold cursor, flat surface) -------------------------

// Phase 4 (P1): the flat outlined search field now lives in the Customer
// design system as `CustomerSearchField` — identical visual, RTL, focus and
// keyboard behaviour, just shared with SalonListScreen instead of copied.
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) = CustomerSearchField(
    value = value,
    onValueChange = onValueChange,
    placeholder = "نام سالن را جستجو کنید...",
    modifier = modifier,
)

// --- Result row -----------------------------------------------------------

@Composable
private fun SearchResultRow(
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
                .rojanPressable(onClick = onClick)
                .padding(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = null,
                tint = HomeColors.TextMuted,
                modifier = Modifier.size(20.dp),
            )
            if (isFollowing) {
                Spacer(Modifier.width(RojanDimens.SpaceSM))
                Icon(
                    Icons.Outlined.NotificationsActive,
                    contentDescription = "دنبال‌شده",
                    tint = CustomerAccent,
                    modifier = Modifier.size(16.dp),
                )
            }
            if (isFavorite) {
                Spacer(Modifier.width(RojanDimens.SpaceXS))
                Icon(
                    Icons.Outlined.FavoriteBorder,
                    contentDescription = "مورد علاقه",
                    tint = CustomerAccent,
                    modifier = Modifier.size(16.dp),
                )
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
                    .size(44.dp)
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
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
            }
        }
    }
}
