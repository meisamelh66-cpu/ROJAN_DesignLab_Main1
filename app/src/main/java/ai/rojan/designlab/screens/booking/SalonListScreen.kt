package ai.rojan.designlab.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.text.withDirectionFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.salon.SalonListViewModel
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.screens.customer.hometheme.HomeTextField
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.image.RojanRemoteImage
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.interaction.rojanPressedShadow
import ai.rojan.designlab.ui.components.loading.RojanSkeletonBox
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.state.RojanEmptyState
import ai.rojan.designlab.ui.components.state.RojanErrorState
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.theme.salonAccentColorFor

private const val SEARCH_DEBOUNCE_MS = 350L

/**
 * Booking Experience Refactor, spec section 9 — Salon Cards.
 *
 * Salon Discovery completion: real backend search (debounced, replaces the
 * old client-side-only filter of a single fixed batch), pagination
 * ("load more" on scroll, via [SalonListViewModel.loadMore]), skeleton
 * loading, and real logo/favorite/follow indicators on each card.
 *
 * Two real data gaps remain, both deliberate rather than papered over:
 * 1. No rating/review-aggregate exists on the backend (no reviews system
 *    server-side) — never shown, never fabricated.
 * 2. No distance/open-now shown on the card — the backend has no geo-radius
 *    query and this app has no on-device location source to compute a real
 *    distance from; "open now" would need one working-hours call per
 *    salon in the list (an N+1 pattern this pass deliberately avoids -
 *    see [ai.rojan.designlab.screens.salon.SalonDetailsScreen], which
 *    *does* show it, using the one working-hours call it already makes
 *    for a single salon). "نزدیک من" stays visible (removing a control
 *    is its own visible change) but remains a documented no-op.
 * 3. [selectedServiceIds] — the entry point from the booking flow's
 *    services-first path — still can't filter salons by "which salons
 *    offer every one of these services" (no backend equivalent of that
 *    cross-salon lookup exists) — always browses all active salons,
 *    same disclosed gap as before this pass.
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
            )
        },
    ),
) {
    // Protected Route Handling fix: an anonymous customer redirected to AUTH
    // from here returns to this exact NavBackStackEntry - same ViewModel
    // instance, still holding its stale pre-login 401 error. Without this,
    // "do not lose user intent" would only be half true: the user is back
    // on the right screen, but it's still showing "please log in" even
    // though they just did. Retrying on every resume (guarded by
    // isUnauthorized, so it's a no-op on the ordinary first-launch resume)
    // picks the real data back up automatically.
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        if (viewModel.isUnauthorized) {
            viewModel.retry()
        }
        onPauseOrDispose { }
    }

    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(SalonSortOption.ALL) }
    var isFirstComposition by remember { mutableStateOf(true) }

    LaunchedEffect(searchQuery) {
        if (isFirstComposition) {
            isFirstComposition = false
        } else {
            delay(SEARCH_DEBOUNCE_MS)
            viewModel.load(searchQuery.takeIf { it.isNotBlank() })
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(listState, viewModel.canLoadMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val total = (viewModel.state as? UiState.Success)?.data?.size ?: return@collect
                if (lastVisibleIndex != null && lastVisibleIndex >= total - 4) {
                    viewModel.loadMore()
                }
            }
    }

    HomeBackgroundTheme {
        Column(modifier = Modifier.fillMaxSize().padding(RojanDimens.SpaceMD)) {
            if (showBackButton || onBusinessLoginClick != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showBackButton) {
                        GlassBackButton(onClick = onBackClick)
                    } else {
                        Box {}
                    }
                    if (onBusinessLoginClick != null) {
                        HomeGlassSurface(
                            modifier = Modifier.rojanPressable(onClick = onBusinessLoginClick),
                            shape = RojanShapes.Small,
                        ) {
                            Text(
                                text = "ورود کسب‌وکار",
                                style = RojanTypography.Caption,
                                color = HomeColors.TextSecondary,
                                modifier = Modifier.padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceSM),
                            )
                        }
                    }
                }
            }

            Text(
                text = "انتخاب سالن",
                style = RojanTypography.HeroTitle,
                color = HomeColors.TextPrimary,
                modifier = Modifier.padding(vertical = RojanDimens.SpaceMD),
            )

            HomeTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("جستجوی سالن...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = HomeColors.TextSecondary) },
                singleLine = true,
                textStyle = LocalTextStyle.current.withDirectionFor(searchQuery),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = RojanDimens.SpaceMD),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                modifier = Modifier.padding(bottom = RojanDimens.SpaceMD),
            ) {
                SalonFilterChip(
                    label = "همه",
                    selected = sortOption == SalonSortOption.ALL,
                    onClick = { sortOption = SalonSortOption.ALL },
                )
                SalonFilterChip(
                    label = "نزدیک من",
                    selected = sortOption == SalonSortOption.NEAREST,
                    onClick = { sortOption = SalonSortOption.NEAREST },
                )
            }

            when (val state = viewModel.state) {
                is UiState.Loading -> SalonListSkeleton()
                is UiState.Error -> if (viewModel.isUnauthorized && onLoginRequired != null) {
                    RojanErrorState(
                        title = "برای مشاهده سالن‌ها وارد شوید",
                        description = state.message,
                        actionLabel = "ورود",
                        onAction = onLoginRequired,
                    )
                } else {
                    RojanErrorState(
                        description = state.message,
                        actionLabel = "تلاش مجدد",
                        onAction = { viewModel.retry() },
                    )
                }
                is UiState.Empty -> RojanEmptyState(
                    title = if (searchQuery.isBlank()) "سالنی یافت نشد" else "سالنی با این جستجو یافت نشد",
                    icon = Icons.Filled.Storefront,
                )
                is UiState.Success -> {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
                    ) {
                        itemsIndexed(state.data) { index, salon ->
                            SalonCard(
                                salon = salon,
                                isFollowing = viewModel.followedSalonIds.contains(salon.id),
                                isFavorite = viewModel.favoriteSalonIds.contains(salon.id),
                                onClick = { onSalonSelected(salon.id) },
                                animationDelayMillis = index * 60,
                            )
                        }
                        if (viewModel.isLoadingMore) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = HomeColors.Glow, strokeWidth = 2.dp)
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

@Composable
private fun SalonFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    HomeGlassSurface(
        modifier = Modifier.rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
        glassAlpha = if (selected) 0.55f else 0.40f,
    ) {
        Text(
            text = label,
            style = RojanTypography.Caption,
            color = if (selected) HomeColors.Glow else HomeColors.TextSecondary,
            modifier = Modifier.padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceSM),
        )
    }
}

/**
 * Each card shows the salon's real logo (falls back to the existing
 * color-tinted icon when [Salon.logoUrl] is null/fails to load), name,
 * short description, and follow/favorite indicators — no rating/distance
 * (see this file's own doc comment for why).
 */
@Composable
private fun SalonCard(
    salon: Salon,
    isFollowing: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    animationDelayMillis: Int = 0,
) {
    val interactionSource = remember { MutableInteractionSource() }
    HomeGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .rojanEnterAnimation(delayMillis = animationDelayMillis)
            .rojanPressable(onClick = onClick, interactionSource = interactionSource),
        shape = RojanShapes.Small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(salonAccentColorFor(salon.id).copy(alpha = 0.35f), RojanShapes.Small),
                contentAlignment = Alignment.Center,
            ) {
                RojanRemoteImage(
                    url = salon.logoUrl,
                    contentDescription = salon.name,
                    shape = RojanShapes.Small,
                    modifier = Modifier.fillMaxSize(),
                    fallback = { Icon(Icons.Filled.Storefront, contentDescription = null, tint = HomeColors.TextPrimary) },
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    salon.name,
                    style = RojanTypography.Body.rojanPressedShadow(interactionSource),
                    color = HomeColors.TextPrimary,
                )
                salon.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        description,
                        style = RojanTypography.Caption,
                        color = HomeColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (isFollowing || isFavorite) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS)) {
                    if (isFollowing) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = "دنبال شده", tint = HomeColors.Glow, modifier = Modifier.size(18.dp))
                    }
                    if (isFavorite) {
                        Icon(Icons.Filled.Favorite, contentDescription = "مورد علاقه", tint = HomeColors.Glow, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/** Loading state: a handful of row-shaped shimmer placeholders matching [SalonCard]'s real layout, via the existing (previously unused anywhere) [RojanSkeletonBox] primitive. */
@Composable
private fun SalonListSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD)) {
        repeat(5) {
            HomeGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
                ) {
                    RojanSkeletonBox(modifier = Modifier.size(72.dp), shape = RojanShapes.Small)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS)) {
                        RojanSkeletonBox(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp))
                        RojanSkeletonBox(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp))
                    }
                }
            }
        }
    }
}
