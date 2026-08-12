package ai.rojan.designlab.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.SolidColor
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.domain.usecase.relationship.GetFavoriteSalonsUseCase
import ai.rojan.designlab.domain.usecase.relationship.GetFollowedSalonsUseCase
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.salon.SalonListViewModel
import ai.rojan.designlab.presentation.salon.SalonListViewModelFactory
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.image.RojanRemoteImage
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.loading.RojanSkeletonBox
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.state.RojanEmptyState
import ai.rojan.designlab.ui.components.state.RojanErrorState
import ai.rojan.designlab.ui.theme.RojanAquaMint
import ai.rojan.designlab.ui.theme.RojanBlushPink
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanPearlPink
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanSoftLavender
import ai.rojan.designlab.ui.theme.RojanTypography

private const val SEARCH_DEBOUNCE_MS = 350L

/**
 * Journey 1, Screen 1: Search.
 *
 * Salon Discovery completion: real debounced backend search (`GET
 * /api/v1/salons?name=`), shared [SalonListViewModel] with
 * [ai.rojan.designlab.screens.booking.SalonListScreen] — same real,
 * disclosed gaps as that screen (no rating/distance/reviews; salon-name
 * is the only search field the backend supports — no service-name/
 * specialist-name/location search exists server-side, confirmed).
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

    LaunchedEffect(query) {
        if (isFirstComposition) {
            isFirstComposition = false
        } else {
            delay(SEARCH_DEBOUNCE_MS)
            viewModel.load(query.takeIf { it.isNotBlank() })
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

    val results = (viewModel.state as? UiState.Success)?.data.orEmpty()

    HomeBackgroundTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassBackButton(onClick = onBackClick)
                Spacer(modifier = Modifier.size(RojanDimens.SpaceMD))
                Text(
                    text = "جستجو",
                    style = RojanTypography.HeroTitle,
                    color = HomeColors.TextPrimary,
                )
            }

            Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))

            HomeGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RojanShapes.Small,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(RojanDimens.SpaceMD),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = HomeColors.Glow,
                    )
                    Spacer(modifier = Modifier.size(RojanDimens.SpaceSM))

                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = RojanTypography.Body.copy(color = HomeColors.TextPrimary).withDirectionFor(query),
                        cursorBrush = SolidColor(HomeColors.Glow),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "نام سالن را جستجو کنید...",
                                    style = RojanTypography.Body,
                                    color = HomeColors.TextMuted,
                                )
                            }
                            inner()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

            if (viewModel.state !is UiState.Loading) {
                Text(
                    text = "نتایج (${results.size})",
                    style = RojanTypography.Body,
                    color = HomeColors.TextSecondary,
                )

                Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))
            }

            when (val state = viewModel.state) {
                is UiState.Loading -> SearchResultsSkeleton()
                is UiState.Error -> if (viewModel.isUnauthorized && onLoginRequired != null) {
                    RojanErrorState(
                        title = "برای جستجوی سالن‌ها وارد شوید",
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
                    title = "نتیجه‌ای یافت نشد",
                    description = "نام سالن دیگری را جستجو کنید",
                    icon = Icons.Filled.Search,
                )
                is UiState.Success -> {
                    LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                        itemsIndexed(state.data) { index, salon ->
                            SearchResultRow(
                                salon = salon,
                                isFollowing = viewModel.followedSalonIds.contains(salon.id),
                                isFavorite = viewModel.favoriteSalonIds.contains(salon.id),
                                onClick = { onSalonClick(salon.id) },
                                animationDelayMillis = index * 60,
                            )
                        }
                        if (viewModel.isLoadingMore) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = HomeColors.Glow, strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    salon: Salon,
    isFollowing: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    animationDelayMillis: Int = 0,
) {
    HomeGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .rojanEnterAnimation(delayMillis = animationDelayMillis)
            .rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(colorSeedFor(salon.id).copy(alpha = 0.5f), RojanShapes.Small),
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

            Spacer(modifier = Modifier.size(RojanDimens.SpaceSM))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = salon.name,
                    style = RojanTypography.Body,
                    color = HomeColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                salon.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        style = RojanTypography.Caption,
                        color = HomeColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (isFollowing) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = "دنبال شده", tint = HomeColors.Glow, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(RojanDimens.SpaceXS))
            }
            if (isFavorite) {
                Icon(Icons.Filled.Favorite, contentDescription = "مورد علاقه", tint = HomeColors.Glow, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(RojanDimens.SpaceXS))
            }

            // RTL/LTR Foundation Readiness: this app is RTL-first with
            // a fixed reading direction (never flips LocalLayoutDirection
            // — see ai.rojan.designlab.ui.text.Text's own doc comment),
            // so a "this row leads forward" chevron always reads left,
            // not conditionally — same reasoning already documented at
            // ai.rojan.designlab.screens.bookingflow.BookingConfirmationScreen's
            // identical chevron.
            Icon(
                imageVector = Icons.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = HomeColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun SearchResultsSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
        repeat(6) {
            HomeGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RojanSkeletonBox(modifier = Modifier.size(56.dp), shape = RojanShapes.Small)
                    Spacer(modifier = Modifier.size(RojanDimens.SpaceSM))
                    RojanSkeletonBox(modifier = Modifier.fillMaxWidth(0.5f).height(14.dp))
                }
            }
        }
    }
}

/** Deterministic per-salon tint — the backend has no per-salon color/branding concept. Same palette/approach as SalonListScreen.kt's SalonCard. */
private val salonCardPalette = listOf(RojanSoftLavender, RojanAquaMint, RojanBlushPink, RojanPearlPink)
private fun colorSeedFor(salonId: String) = salonCardPalette[Math.floorMod(salonId.hashCode(), salonCardPalette.size)]
