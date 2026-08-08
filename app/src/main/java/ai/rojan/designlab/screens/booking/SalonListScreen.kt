package ai.rojan.designlab.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.text.withDirectionFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.salon.SalonListViewModel
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.screens.customer.hometheme.HomeTextField
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.interaction.rojanPressedShadow
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.state.RojanEmptyState
import ai.rojan.designlab.ui.components.state.RojanErrorState
import ai.rojan.designlab.ui.components.state.RojanLoadingState
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.theme.salonAccentColorFor

/**
 * Booking Experience Refactor, spec section 9 — Salon Cards.
 * "Each card contains ONLY: Cover Image, Salon Name, Rating, Distance."
 *
 * **Android <-> Backend Full Integration milestone:** now backed by
 * [SalonListViewModel] -> `GET /api/v1/salons` instead of
 * `DemoSalonRepository`. Two real data gaps from this swap, both
 * deliberate rather than papered over with fabricated data:
 *
 * 1. The backend's `Salon` has no rating/review-aggregate or distance/
 *    geolocation concept (no reviews system, no location tracking exist
 *    server-side yet) — [MinimalSalonCard] no longer renders those two
 *    rows rather than showing a fake number. "نزدیک من" stays visible
 *    (removing a button is its own visible change) but is now a no-op —
 *    there is no real distance to sort by.
 * 2. [selectedServiceIds] — the entry point from the booking flow's
 *    services-first path — can no longer filter salons by "which salons
 *    offer every one of these services": the backend models a `Service`
 *    as belonging to exactly one salon (via its category), not as one
 *    global service spanning several salons the way
 *    [ai.rojan.designlab.data.demo.DemoService.offeredBySalonIds] did.
 *    There is no backend equivalent of that cross-salon lookup. Rather
 *    than fabricate a matching algorithm the backend doesn't support,
 *    this now always browses all active salons regardless of
 *    [selectedServiceIds] — flagged as a real product/backend mismatch
 *    for a future decision (e.g. a dedicated search endpoint, or making
 *    the booking flow salon-first), not a redesign of the flow itself.
 */
@Composable
fun SalonListScreen(
    selectedServiceIds: List<String>,
    onBackClick: () -> Unit,
    onSalonSelected: (String) -> Unit,
    showBackButton: Boolean = true,
    onBusinessLoginClick: (() -> Unit)? = null,
    viewModel: SalonListViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = ai.rojan.designlab.presentation.salon.SalonListViewModelFactory(
            ai.rojan.designlab.di.BackendApiContainerHolder.get(
                androidx.compose.ui.platform.LocalContext.current,
            ).salonRepository,
        ),
    ),
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(SalonSortOption.ALL) }

    val loadedSalons = (viewModel.state as? UiState.Success)?.data.orEmpty()
    val displayedSalons = remember(loadedSalons, searchQuery) {
        if (searchQuery.isBlank()) {
            loadedSalons
        } else {
            loadedSalons.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }
    // "نزدیک من" has no real distance to sort by (see class doc) - kept as a visible, inert option.
    val sortedSalons = displayedSalons

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
                is UiState.Loading -> RojanLoadingState(message = "در حال بارگذاری سالن‌ها...")
                is UiState.Error -> RojanErrorState(
                    description = state.message,
                    actionLabel = "تلاش مجدد",
                    onAction = { viewModel.retry() },
                )
                is UiState.Empty -> RojanEmptyState(
                    title = "سالنی یافت نشد",
                    icon = Icons.Filled.Storefront,
                )
                is UiState.Success -> {
                    if (sortedSalons.isEmpty()) {
                        RojanEmptyState(
                            title = "سالنی با این جستجو یافت نشد",
                            icon = Icons.Filled.Storefront,
                        )
                    }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD)) {
                        itemsIndexed(sortedSalons) { index, salon ->
                            MinimalSalonCard(
                                salon = salon,
                                onClick = { onSalonSelected(salon.id) },
                                animationDelayMillis = index * 60,
                            )
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
 * The backend's `Salon` has no rating/review-aggregate or distance concept
 * (see [SalonListScreen]'s doc comment) — this card shows only what's real:
 * a color-tinted placeholder (no image URL from the backend either) and the
 * salon's name.
 */
@Composable
private fun MinimalSalonCard(salon: Salon, onClick: () -> Unit, animationDelayMillis: Int = 0) {
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
                Icon(Icons.Filled.Storefront, contentDescription = null, tint = HomeColors.TextPrimary)
            }

            Column {
                Text(
                    salon.name,
                    style = RojanTypography.Body.rojanPressedShadow(interactionSource),
                    color = HomeColors.TextPrimary,
                )
            }
        }
    }
}
