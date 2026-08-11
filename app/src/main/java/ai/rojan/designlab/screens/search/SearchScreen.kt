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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.text.withDirectionFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.SolidColor
import androidx.lifecycle.viewmodel.compose.viewModel

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.salon.SalonListViewModel
import ai.rojan.designlab.presentation.salon.SalonListViewModelFactory
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
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

/**
 * Journey 1, Screen 1: Search.
 *
 * **Android <-> Backend Full Integration milestone:** now backed by the
 * same [SalonListViewModel] -> `GET /api/v1/salons` [ai.rojan.designlab.screens.booking.SalonListScreen]
 * uses, filtered client-side by name — replaces the old `CatalogEngine()`
 * -> `DemoSalonRepository` read path, whose demo salon ids no longer
 * resolved once Salon Details/booking started reading real backend ids
 * (a real id from here would 404 downstream). Same real-data gaps as
 * [ai.rojan.designlab.screens.booking.SalonListScreen]: no rating/
 * distance concept server-side, so those rows are gone rather than
 * fabricated.
 */
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onSalonClick: (String) -> Unit,
    onLoginRequired: (() -> Unit)? = null,
    viewModel: SalonListViewModel = viewModel(
        factory = SalonListViewModelFactory(
            BackendApiContainerHolder.get(LocalContext.current).salonRepository,
        ),
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

    val loadedSalons = (viewModel.state as? UiState.Success)?.data.orEmpty()
    val results = remember(loadedSalons, query) {
        if (query.isBlank()) loadedSalons else loadedSalons.filter { it.name.contains(query, ignoreCase = true) }
    }

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
                                    text = "نام سالن یا خدمت را جستجو کنید...",
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

            Text(
                text = "نتایج (${results.size})",
                style = RojanTypography.Body,
                color = HomeColors.TextSecondary,
            )

            Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))

            when (val state = viewModel.state) {
                is UiState.Loading -> RojanLoadingState(message = "در حال بارگذاری سالن‌ها...")
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
                else -> if (results.isEmpty()) {
                RojanEmptyState(
                    title = "نتیجه‌ای یافت نشد",
                    description = "سالن یا خدمت دیگری را جستجو کنید",
                    icon = Icons.Filled.Search,
                )
            } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                itemsIndexed(results) { index, salon ->
                    HomeGlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .rojanEnterAnimation(delayMillis = index * 60)
                            .rojanPressable(onClick = { onSalonClick(salon.id) }),
                        shape = RojanShapes.Small,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(RojanDimens.SpaceMD),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Real backend Salon has no color/image concept — same
                            // deterministic-tint fallback SalonListScreen.kt's
                            // MinimalSalonCard already established for this reason.
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(colorSeedFor(salon.id).copy(alpha = 0.5f), RojanShapes.Small),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Storefront,
                                    contentDescription = null,
                                    tint = HomeColors.TextPrimary,
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
                            }

                            // RTL/LTR Foundation Readiness: this app is RTL-first with
                            // a fixed reading direction (never flips LocalLayoutDirection
                            // — see ai.rojan.designlab.ui.text.Text's own doc comment),
                            // so a "this row leads forward" chevron always reads left,
                            // not conditionally — same reasoning already documented at
                            // ai.rojan.designlab.screens.bookingflow.BookingConfirmationScreen's
                            // identical chevron. Icons.Filled.ArrowForward (pointing
                            // right) was backwards against that convention; corrected
                            // to match.
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = HomeColors.TextSecondary,
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

/** Deterministic per-salon tint — the backend has no per-salon color/branding concept. Same palette/approach as SalonListScreen.kt's MinimalSalonCard. */
private val salonCardPalette = listOf(RojanSoftLavender, RojanAquaMint, RojanBlushPink, RojanPearlPink)
private fun colorSeedFor(salonId: String) = salonCardPalette[Math.floorMod(salonId.hashCode(), salonCardPalette.size)]
