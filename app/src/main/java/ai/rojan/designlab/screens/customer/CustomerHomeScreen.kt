package ai.rojan.designlab.screens.customer

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.presentation.auth.AuthViewModel
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.salon.SalonListViewModel
import ai.rojan.designlab.presentation.salon.SalonListViewModelFactory
import ai.rojan.designlab.screens.customer.components.CustomerAccent as RefAccent
import ai.rojan.designlab.screens.customer.components.CustomerCardShape as RefCardShape
import ai.rojan.designlab.screens.customer.components.CustomerHairline as RefHairline
import ai.rojan.designlab.screens.customer.components.CustomerOnAccent as RefOnAccent
import ai.rojan.designlab.screens.customer.components.CustomerScreenMargin as RefScreenMargin
import ai.rojan.designlab.screens.customer.components.CustomerSectionLabelStyle as RefSectionLabelStyle
import ai.rojan.designlab.screens.customer.components.CustomerSurfaceFill as RefSurfaceFill
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.components.image.RojanRemoteImage
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

/* =============================================================================
 * ROJAN Customer — EXPLORE / salon discovery (route RojanDestinations.EXPLORE,
 * the bottom bar's "جستجو" tab).
 *
 * Adopts the approved "quiet luxury" reference (REFERENCE-SPEC-salon-detail.md,
 * REFERENCE-SPEC-customer-home.md). Everything visual here is screen-local and
 * `private`. This file changes NO ViewModel, repository, domain, API, auth,
 * navigation graph, route, or backend contract, and edits NO shared component /
 * design-system token / colour / icon. [CustomerHomeScreen]'s public signature
 * and its RojanNavGraph wiring are byte-identical, and the shared
 * [CustomerBottomBar] is used unchanged.
 *
 * Repositioned from a 13-section marketplace dashboard to a focused salon
 * discovery screen: clean Persian title → one flat search affordance (opens
 * the live [ai.rojan.designlab.screens.search.SearchScreen] via `onSearchClick`,
 * unchanged) → a single vertical list of real salons from the existing
 * [SalonListViewModel] (`GET /api/v1/salons`, the same call [FeaturedSalons]
 * made) → calm loading / empty / error states.
 *
 * Removed from the composition (NOT deleted — the composables stay in the tree,
 * just no longer called here): the glass [HomeHeader], [AISearchBar],
 * [SearchModeTabs] (a toggle that fed nothing), the 360dp
 * [ai.rojan.designlab.components.hero.HeroBookingCard], and the six
 * placeholder / "به‌زودی" sections ([PopularServices], [TopSpecialists],
 * [PromotionsSection], [NearbySalons], [RecommendedSalons], [FollowedSalons])
 * plus the two activity sections ([UpcomingBookings], [RecentVisits]) that
 * belong on Home, not on a discovery screen. `onBookAppointmentClick` /
 * `onViewAllServicesClick` / `onSpecialistClick` are no longer surfaced here —
 * the salon cards are the discovery-and-booking path (tap → Salon Detail).
 * ========================================================================== */

// Phase 4 (P1) token consolidation: RefScreenMargin / RefCardRadius / RefAccent
// / RefSurfaceFill / RefHairline / RefCardShape / RefSectionLabelStyle /
// RefOnAccent were literal duplicates of the Customer* design tokens — they are
// now import aliases of those (see imports above), so the values live only in
// CustomerRefComponents. The two below have no Customer* equivalent (the tile
// scale/fill is unique to this screen) and stay local.
private val RefTileRadius = 12.dp
private val RefTileFill = Color.White.copy(alpha = 0.05f)

/**
 * The Customer Explore / salon-discovery screen. Renders at
 * [ai.rojan.designlab.navigation.RojanDestinations.EXPLORE] — the bottom bar's
 * "جستجو" destination, and the guest landing screen. Signature, every `on*`
 * callback, and [bottomBarActiveTab] are unchanged; the caller (RojanNavGraph)
 * still decides which tab reads as active.
 */
@Composable
fun CustomerHomeScreen(
    authViewModel: AuthViewModel,
    onProfileClick: () -> Unit = {},
    onBookAppointmentClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSalonClick: (String) -> Unit = {},
    onHomeClick: () -> Unit = {},
    onViewAllServicesClick: () -> Unit = {},
    onSpecialistClick: (String) -> Unit = {},
    onLoginClick: () -> Unit = {},
    bottomBarActiveTab: CustomerHomeTab = CustomerHomeTab.SEARCH,
) {
    val container = BackendApiContainerHolder.get(LocalContext.current)

    // Explore is shown to guests (first launch / after logout), so salon
    // discovery must not require auth: SalonListViewModel routes browsing to
    // the public directory (GET /api/v1/public/salons) whenever no token
    // exists, and to the authenticated GET /api/v1/salons otherwise.
    val salonListViewModel: SalonListViewModel = viewModel(
        factory = SalonListViewModelFactory(
            salonRepository = container.salonRepository,
            publicSalonRepository = container.publicSalonRepository,
            hasSession = { container.tokenRepository.accessToken()?.isNotBlank() == true },
        ),
    )

    val salonState = salonListViewModel.state
    val salons = (salonState as? UiState.Success)?.data.orEmpty()

    HomeBackgroundTheme(
        modifier = Modifier.fillMaxSize(),
        // Insets handled locally: ExploreHeader takes statusBarsPadding. The
        // persistent bottom bar now lives in CustomerMainScaffold (a sibling
        // below this content), so this list no longer scrolls under it and
        // no longer needs to pad its bottom by the bar's height.
        applyContentInsets = false,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = RojanDimens.SpaceLG,
                bottom = RojanDimens.SpaceLG,
            ),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceLG),
        ) {
                item { ExploreHeader(onProfileClick = onProfileClick) }

                item { ExploreSearchBar(onClick = onSearchClick) }

                item { RefSectionLabel("سالن‌ها") }

                when {
                    salonState is UiState.Loading -> {
                        items(4) {
                            ExploreSalonSkeleton(
                                modifier = Modifier.padding(horizontal = RefScreenMargin),
                            )
                        }
                    }

                    salons.isNotEmpty() -> {
                        items(salons, key = { it.id }) { salon ->
                            ExploreSalonCard(
                                salon = salon,
                                modifier = Modifier.padding(horizontal = RefScreenMargin),
                                onClick = { onSalonClick(salon.id) },
                            )
                        }
                    }

                    salonState is UiState.Error && salonListViewModel.isUnauthorized -> item {
                        // Defensive: browsing is public, so a guest should never
                        // reach here — but if an authenticated browse 401s
                        // (token revoked mid-session), offer login rather than a
                        // dead-end retry.
                        ExploreMessage(
                            icon = Icons.AutoMirrored.Outlined.Login,
                            title = "برای مشاهده سالن‌ها وارد شوید",
                            body = salonState.message,
                            actionLabel = "ورود",
                            onAction = onLoginClick,
                        )
                    }

                    salonState is UiState.Error -> item {
                        ExploreMessage(
                            icon = Icons.Outlined.CloudOff,
                            title = "مشکلی پیش آمد",
                            body = salonState.message,
                            actionLabel = "تلاش مجدد",
                            onAction = salonListViewModel::retry,
                        )
                    }

                    else -> item {
                        ExploreMessage(
                            icon = Icons.Outlined.SearchOff,
                            title = "سالنی یافت نشد",
                            body = "در حال حاضر سالنی برای نمایش وجود ندارد.",
                        )
                    }
                }
        }
    }
}

// --- Header --------------------------------------------------------------

@Composable
private fun ExploreHeader(onProfileClick: () -> Unit) {
    Column(modifier = Modifier.statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RefScreenMargin, vertical = RojanDimens.SpaceSM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(
                    "کشف سالن‌ها",
                    style = RojanTypography.SectionTitle,
                    color = HomeColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(RojanDimens.SpaceXS))
                Text(
                    "سالن مناسب خود را پیدا کنید",
                    style = RojanTypography.Caption,
                    color = HomeColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(RojanDimens.SpaceMD))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(RefSurfaceFill)
                    .border(1.dp, RefHairline, CircleShape)
                    .rojanPressable(onClick = onProfileClick, role = Role.Button),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = "پروفایل",
                    tint = HomeColors.TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(RojanDimens.SpaceMD))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(RefHairline),
        )
    }
}

// --- Search entry (opens the live SearchScreen — behaviour unchanged) ------

@Composable
private fun ExploreSearchBar(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RefScreenMargin)
            .clip(RefCardShape)
            .background(RefSurfaceFill)
            .border(1.dp, RefHairline, RefCardShape)
            .rojanPressable(onClick = onClick, role = Role.Button),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = HomeColors.TextMuted,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(RojanDimens.SpaceSM))
            Text(
                "جستجوی سالن، خدمت یا متخصص…",
                style = RojanTypography.Caption,
                color = HomeColors.TextMuted,
            )
        }
    }
}

@Composable
private fun RefSectionLabel(text: String) {
    Text(
        text,
        style = RefSectionLabelStyle,
        color = HomeColors.TextMuted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RefScreenMargin),
    )
}

// --- Salon card -------------------------------------------------------

@Composable
private fun ExploreSalonCard(
    salon: Salon,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RefCardShape)
            .background(RefSurfaceFill)
            .border(1.dp, RefHairline, RefCardShape)
            .rojanPressable(onClick = onClick, role = Role.Button),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = null,
                tint = HomeColors.TextMuted,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(RojanDimens.SpaceSM))

            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(
                    salon.name,
                    style = RojanTypography.CardTitle,
                    color = HomeColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                salon.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Spacer(Modifier.height(RojanDimens.SpaceXS))
                    Text(
                        description,
                        style = RojanTypography.Caption,
                        color = HomeColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                salon.address.takeIf { it.isNotBlank() }?.let { address ->
                    Spacer(Modifier.height(RojanDimens.SpaceXS))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Place,
                            contentDescription = null,
                            tint = HomeColors.TextMuted,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(RojanDimens.SpaceXS))
                        Text(
                            address,
                            style = RojanTypography.Caption,
                            color = HomeColors.TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(Modifier.width(RojanDimens.SpaceMD))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(RefTileRadius))
                    .background(RefTileFill),
                contentAlignment = Alignment.Center,
            ) {
                RojanRemoteImage(
                    url = salon.logoUrl,
                    contentDescription = salon.name,
                    shape = RoundedCornerShape(RefTileRadius),
                    modifier = Modifier.fillMaxSize(),
                    fallback = {
                        Icon(
                            Icons.Outlined.Storefront,
                            contentDescription = null,
                            tint = HomeColors.TextSecondary,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ExploreSalonSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RefCardShape)
            .background(RefSurfaceFill)
            .border(1.dp, RefHairline, RefCardShape)
            .alpha(0.6f),
    )
}

// --- Calm empty / error state (screen-local — shared RojanEmptyState /
//     RojanErrorState left untouched for other screens) ------------------

@Composable
private fun ExploreMessage(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RefScreenMargin, vertical = RojanDimens.SpaceXXL),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = HomeColors.TextMuted, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(RojanDimens.SpaceMD))
        Text(title, style = RojanTypography.CardTitle, color = HomeColors.TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(RojanDimens.SpaceXS))
        Text(body, style = RojanTypography.Caption, color = HomeColors.TextSecondary, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(RojanDimens.SpaceLG))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(RefTileRadius))
                    .background(RefAccent)
                    .rojanPressable(onClick = onAction, role = Role.Button)
                    .padding(horizontal = RojanDimens.SpaceLG, vertical = RojanDimens.SpaceSM),
            ) {
                Text(actionLabel, style = RojanTypography.Button, color = RefOnAccent)
            }
        }
    }
}
