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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.domain.repository.BookingWithDetails
import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.presentation.auth.AuthViewModel
import ai.rojan.designlab.presentation.booking.BookingHistoryViewModel
import ai.rojan.designlab.presentation.booking.BookingHistoryViewModelFactory
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.salon.SalonListViewModel
import ai.rojan.designlab.presentation.salon.SalonListViewModelFactory
import ai.rojan.designlab.screens.customer.components.CustomerAccent as RefAccent
import ai.rojan.designlab.screens.customer.components.CustomerButtonHeight as RefButtonHeight
import ai.rojan.designlab.screens.customer.components.CustomerButtonRadius as RefButtonRadius
import ai.rojan.designlab.screens.customer.components.CustomerCardRadius as RefCardRadius
import ai.rojan.designlab.screens.customer.components.CustomerCardShape as RefCardShape
import ai.rojan.designlab.screens.customer.components.CustomerHairline as RefHairline
import ai.rojan.designlab.screens.customer.components.CustomerOnAccent as RefOnAccent
import ai.rojan.designlab.screens.customer.components.CustomerScreenMargin as RefScreenMargin
import ai.rojan.designlab.screens.customer.components.CustomerSectionLabelStyle as RefSectionLabelStyle
import ai.rojan.designlab.screens.customer.components.CustomerSurfaceFill as RefSurfaceFill
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

/* =============================================================================
 * ROJAN Customer — HOME (post-auth landing, route RojanDestinations.CUSTOMER_HOME).
 *
 * Adopts the approved "quiet luxury" visual reference:
 *   docs/design-review/customer/REFERENCE-SPEC-salon-detail.md   (golden reference)
 *   docs/design-review/customer/REFERENCE-SPEC-customer-home.md   (this screen)
 *
 * Everything visual here is screen-local and `private`. This file changes NO
 * ViewModel, repository, domain, API, navigation graph, auth, or backend
 * contract, and edits NO shared component / design-system token / colour /
 * icon. [CustomerDashboardScreen]'s public signature and its RojanNavGraph
 * wiring are byte-identical.
 *
 * `CustomerHomeScreen` (route EXPLORE, the "جستجو" tab) is a different screen
 * and is untouched — the shared sections it still uses (HomeHeader,
 * HeroBookingCard, FeaturedSalons, AISearchBar, PromotionsSection,
 * CustomerBottomBar, …) are left exactly as they were.
 *
 * Removed from Home vs. the previous implementation: the glass [HomeHeader],
 * the 360dp [ai.rojan.designlab.components.hero.HeroBookingCard] (AI photo +
 * gradient pill), the [ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface]
 * search entry, the tinted-glass [FeaturedSalons] card, and the three
 * `RojanComingSoonState` sections ([RecommendedSalons] / [TopSpecialists] /
 * [FollowedSalons] — none has a backend behind it; each only drew a "به‌زودی"
 * card). The bottom bar is the shared [CustomerBottomBar], rebuilt flat in the
 * Quiet Luxury pass (the protruding glowing Home disc is gone) — one visual
 * implementation, shared with [CustomerHomeScreen].
 *
 * Kept: [HomeBackgroundTheme] dark ground, every real ViewModel + backend
 * field (real salons, real bookings, real display name), and every on*
 * callback. Nothing fabricated, nothing mocked, no real feature dropped.
 * ========================================================================== */

// Phase 4 (P1) token consolidation: RefScreenMargin / RefCardRadius /
// RefButtonRadius / RefButtonHeight / RefAccent / RefOnAccent / RefSurfaceFill
// / RefHairline / RefCardShape / RefSectionLabelStyle were literal duplicates
// of the Customer* design tokens and are now import aliases of those (see
// imports above) — the values live only in CustomerRefComponents. RefTileFill
// has no Customer* equivalent and stays local.
private val RefTileFill = Color.White.copy(alpha = 0.05f)

/**
 * ROJAN AI Customer Home Dashboard — the customer's first impression after
 * Splash → Authentication (bottom bar's "خانه" tab).
 *
 * Signature, callbacks, and nav-graph wiring are unchanged from the previous
 * implementation; only the presentation is rebuilt to the approved reference.
 * The two ViewModels are hoisted here (as [BookingHistoryViewModel] already
 * was) so the screen can decide whether a section has real content before it
 * emits the section at all — a placeholder-free feed.
 */
@Composable
fun CustomerDashboardScreen(
    authViewModel: AuthViewModel,
    onProfileClick: () -> Unit = {},
    onBookAppointmentClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSalonClick: (String) -> Unit = {},
) {
    val container = BackendApiContainerHolder.get(LocalContext.current)

    val salonListViewModel: SalonListViewModel = viewModel(
        factory = SalonListViewModelFactory(container.salonRepository),
    )
    // One `GET /bookings/mine` backs both the upcoming and the recent section.
    val bookingHistoryViewModel: BookingHistoryViewModel = viewModel(
        factory = BookingHistoryViewModelFactory(container.bookingHistoryRepository),
    )

    val firstName = authViewModel.currentDisplayName
        ?.trim()?.substringBefore(" ")?.takeIf { it.isNotBlank() } ?: "کاربر"

    val salonState = salonListViewModel.state
    val salons = (salonState as? UiState.Success)?.data.orEmpty()

    val bookings = (bookingHistoryViewModel.state as? UiState.Success)?.data.orEmpty()
    val upcoming = bookings.filter {
        it.booking.status == BookingStatus.PENDING || it.booking.status == BookingStatus.CONFIRMED
    }
    val recent = bookings.filter { it.booking.status == BookingStatus.COMPLETED }

    HomeBackgroundTheme(
        modifier = Modifier.fillMaxSize(),
        // Insets handled locally: HomeGreetingRow takes statusBarsPadding. The
        // persistent bottom bar now lives in CustomerMainScaffold (a sibling
        // below this content), so this list no longer scrolls under it and no
        // longer needs to pad its bottom by the bar's height.
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
                item { HomeGreetingRow(firstName = firstName, onProfileClick = onProfileClick) }

                item { HomeSearchBar(onClick = onSearchClick) }

                item {
                    RefPrimaryButton(
                        label = "رزرو نوبت",
                        onClick = onBookAppointmentClick,
                        modifier = Modifier.padding(horizontal = RefScreenMargin),
                    )
                }

                if (salonState is UiState.Loading || salons.isNotEmpty()) {
                    item {
                        HomeSalonSection(
                            loading = salonState is UiState.Loading,
                            salons = salons,
                            onSalonClick = onSalonClick,
                        )
                    }
                }

                if (upcoming.isNotEmpty()) {
                    item {
                        HomeBookingSection(
                            label = "نوبت‌های پیش‌رو",
                            items = upcoming,
                            onSalonClick = onSalonClick,
                        )
                    }
                }

            if (recent.isNotEmpty()) {
                item {
                    HomeBookingSection(
                        label = "بازدیدهای اخیر",
                        items = recent,
                        onSalonClick = onSalonClick,
                    )
                }
            }
        }
    }
}

// --- Flat surface (glass only as a whisper of translucent lift) --------------

@Composable
private fun RefSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RefCardShape)
            .background(RefSurfaceFill)
            .border(1.dp, RefHairline, RefCardShape),
    ) {
        content()
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

// --- Primary CTA (solid rose gold, no gradient) -----------------------------

@Composable
private fun RefPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(RefButtonHeight)
            .clip(RoundedCornerShape(RefButtonRadius))
            .background(RefAccent)
            .rojanPressable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = RojanTypography.Button, color = RefOnAccent)
    }
}

// --- Greeting row (replaces the glass HomeHeader) ---------------------------

@Composable
private fun HomeGreetingRow(firstName: String, onProfileClick: () -> Unit) {
    Column(modifier = Modifier.statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RefScreenMargin, vertical = RojanDimens.SpaceSM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "سلام $firstName جان",
                style = RojanTypography.SectionTitle,
                color = HomeColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
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

// --- Search entry ---------------------------------------------------------

@Composable
private fun HomeSearchBar(onClick: () -> Unit) {
    RefSurface(
        modifier = Modifier
            .padding(horizontal = RefScreenMargin)
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

// --- Suggested salons ---------------------------------------------------

@Composable
private fun HomeSalonSection(
    loading: Boolean,
    salons: List<Salon>,
    onSalonClick: (String) -> Unit,
) {
    Column {
        RefSectionLabel("سالن‌های پیشنهادی")
        Spacer(Modifier.height(RojanDimens.SpaceSM))
        if (loading) {
            Row(
                modifier = Modifier.padding(horizontal = RefScreenMargin),
                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
            ) {
                repeat(2) { HomeSalonSkeleton() }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = RefScreenMargin),
                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
            ) {
                items(salons, key = { it.id }) { salon ->
                    HomeSalonCard(salon) { onSalonClick(salon.id) }
                }
            }
        }
    }
}

@Composable
private fun HomeSalonCard(salon: Salon, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(200.dp)
            .clip(RefCardShape)
            .background(RefSurfaceFill)
            .border(1.dp, RefHairline, RefCardShape)
            .rojanPressable(onClick = onClick, role = Role.Button),
    ) {
        Column(modifier = Modifier.padding(RojanDimens.SpaceMD), horizontalAlignment = Alignment.End) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(RefButtonRadius))
                    .background(RefTileFill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Storefront,
                    contentDescription = null,
                    tint = HomeColors.TextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.height(RojanDimens.SpaceSM))
            Text(
                salon.name,
                style = RojanTypography.Body,
                color = HomeColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
                    salon.address,
                    style = RojanTypography.Caption,
                    color = HomeColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HomeSalonSkeleton() {
    Box(
        modifier = Modifier
            .width(200.dp)
            .height(128.dp)
            .clip(RefCardShape)
            .background(RefSurfaceFill)
            .border(1.dp, RefHairline, RefCardShape)
            .alpha(0.6f),
    )
}

// --- Upcoming / recent bookings (real data; section hidden when empty) ----

@Composable
private fun HomeBookingSection(
    label: String,
    items: List<BookingWithDetails>,
    onSalonClick: (String) -> Unit,
) {
    Column {
        RefSectionLabel(label)
        Spacer(Modifier.height(RojanDimens.SpaceSM))
        Column(
            modifier = Modifier.padding(horizontal = RefScreenMargin),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            items.forEach { item ->
                HomeBookingRow(item) { onSalonClick(item.booking.salonId) }
            }
        }
    }
}

@Composable
private fun HomeBookingRow(item: BookingWithDetails, onClick: () -> Unit) {
    val booking = item.booking
    RefSurface(modifier = Modifier.rojanPressable(onClick = onClick, role = Role.Button)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(RefButtonRadius))
                    .background(RefTileFill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = RefAccent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(RojanDimens.SpaceMD))
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(
                    item.salonName ?: booking.salonId,
                    style = RojanTypography.Body,
                    color = HomeColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.specialistName?.let { name ->
                    Spacer(Modifier.height(RojanDimens.SpaceXS))
                    Text(
                        name,
                        style = RojanTypography.Caption,
                        color = HomeColors.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(RojanDimens.SpaceXS))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = HomeColors.TextMuted,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(RojanDimens.SpaceXS))
                    Text(
                        booking.startTime.substringBefore('T') +
                            "  ·  " +
                            booking.startTime.substringAfter('T').take(5),
                        style = RojanTypography.Caption,
                        color = HomeColors.TextMuted,
                    )
                }
            }
        }
    }
}
