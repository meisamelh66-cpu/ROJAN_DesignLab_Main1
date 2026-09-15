package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush as ColorBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

import ai.rojan.designlab.R
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
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography

/* =============================================================================
 * ROJAN Customer — HOME (post-auth landing, route RojanDestinations.CUSTOMER_HOME).
 *
 * Visual Refresh (2026-09-15): rebuilt to match the approved reference image
 * supplied for this pass (dark purple/navy premium composition — hero banner,
 * service-category shortcuts, a promotional salon banner, and photo-forward
 * recommended-salon cards) while keeping every real ViewModel, backend field,
 * navigation callback, and the shared bottom bar exactly as they were. Nothing
 * in this file changes a ViewModel, repository, domain model, API contract,
 * navigation graph route, auth flow, or backend contract, and no shared
 * design-system token/colour is edited — only this screen's own presentation.
 *
 * Honest data disclosure (no fabricated backend functionality):
 * - [Salon] has no rating/review-count/distance field (see that model's own
 *   doc comment — the backend has no such aggregate). The recommended-salon
 *   cards below intentionally show only real fields (name, address, photo)
 *   and never a star rating, review count, or distance, unlike the reference
 *   image's mockup data.
 * - There is no real "current location" source wired into this screen (no
 *   geolocation/tenant-city call). The header's location row is a real,
 *   present UI control (matches the reference's visual structure) but shows
 *   a neutral, honest label rather than a fabricated city/neighbourhood.
 * - The hero portrait ([HomeHeroPortraitSlot], `R.drawable.hero_customer_portrait`)
 *   is cropped directly from the reference image supplied for this task
 *   (`D:\olgoo rojan`) — the reference's own asset, explicitly authorized for
 *   this use, not a newly generated or fabricated photo. Cropped to the
 *   portrait alone (no status bar/UI chrome from the original mockup).
 * - The promotional banner reuses the already-bundled, real
 *   `R.drawable.bg_master_luxury_salon` photo (existing project asset) — a
 *   genuine premium salon-interior image, not a new fabrication.
 *
 * `CustomerHomeScreen` (route EXPLORE, the "جستجو" tab) is a different screen
 * and is untouched.
 * ========================================================================== */

private val RefTileFill = Color.White.copy(alpha = 0.05f)
private val HeroShape = RoundedCornerShape(24.dp)
private val PromoShape = RoundedCornerShape(20.dp)
private val CategoryTileShape = RoundedCornerShape(16.dp)

private data class HomeCategory(val label: String, val icon: ImageVector)

// Reference composition (top-level "خانه" categories): ناخن / آرایش / مو /
// ماساژ / مراقبت پوست / سایر خدمات. Tapping any tile opens real salon
// discovery (onExploreClick, already-existing navigation) — there is no
// per-category filter endpoint to wire these to individually, so every tile
// honestly leads to the same real "browse salons" destination rather than a
// fabricated category-specific result.
private val homeCategories = listOf(
    HomeCategory("ناخن", Icons.Outlined.Colorize),
    HomeCategory("آرایش", Icons.Outlined.Brush),
    HomeCategory("مو", Icons.Outlined.ContentCut),
    HomeCategory("ماساژ", Icons.Outlined.Spa),
    HomeCategory("مراقبت پوست", Icons.Outlined.Face),
    HomeCategory("سایر خدمات", Icons.Outlined.Apps),
)

/**
 * ROJAN AI Customer Home Dashboard — the customer's first impression after
 * Splash → Authentication (bottom bar's "خانه" tab).
 *
 * Signature is source-compatible with the previous implementation — every
 * existing parameter and callback behaviour is unchanged; [onNotificationsClick]
 * is a new, optional (defaulted) parameter for the reference's notification
 * control, so existing call sites in RojanNavGraph keep compiling unmodified.
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
    onNotificationsClick: () -> Unit = {},
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
        // Insets handled locally: HomeHeroCard takes statusBarsPadding. The
        // persistent bottom bar lives in CustomerMainScaffold (a sibling below
        // this content), so this list doesn't pad its bottom for it.
        applyContentInsets = false,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = RojanDimens.SpaceMD,
                bottom = RojanDimens.SpaceLG,
            ),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceLG),
        ) {
                item {
                    HomeHeroCard(
                        firstName = firstName,
                        onProfileClick = onProfileClick,
                        onNotificationsClick = onNotificationsClick,
                    )
                }

                item {
                    HomeSearchBar(
                        onClick = onSearchClick,
                        modifier = Modifier.padding(horizontal = RefScreenMargin),
                    )
                }

                item { HomeCategoryRow(onCategoryClick = onExploreClick) }

                item {
                    HomePromoBanner(
                        onPrimaryClick = onBookAppointmentClick,
                        modifier = Modifier.padding(horizontal = RefScreenMargin),
                    )
                }

                if (salonState is UiState.Loading || salons.isNotEmpty()) {
                    item {
                        HomeSalonSection(
                            loading = salonState is UiState.Loading,
                            salons = salons,
                            onSalonClick = onSalonClick,
                            onViewAllClick = onExploreClick,
                        )
                    }
                } else if (salonState is UiState.Error) {
                    item {
                        HomeSectionErrorRow(message = salonState.message, onRetry = salonListViewModel::retry)
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

            if (bookingHistoryViewModel.state is UiState.Error && upcoming.isEmpty() && recent.isEmpty()) {
                item {
                    HomeSectionErrorRow(
                        message = (bookingHistoryViewModel.state as UiState.Error).message,
                        onRetry = bookingHistoryViewModel::retry,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSectionErrorRow(message: String, onRetry: () -> Unit) {
    RefSurface(modifier = Modifier.padding(horizontal = RefScreenMargin)) {
        Column(Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD)) {
            Text(message, style = RojanTypography.Body, color = HomeColors.TextSecondary)
            Spacer(Modifier.height(RojanDimens.SpaceSM))
            Text(
                "تلاش مجدد",
                style = RojanTypography.Body,
                color = RefAccent,
                modifier = Modifier
                    .heightIn(min = RojanDimens.MinTouchTarget)
                    .rojanPressable(onClick = onRetry, role = Role.Button),
            )
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
private fun RefSectionLabel(text: String, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RefScreenMargin),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (action != null) action() else Spacer(Modifier.width(0.dp))
        Text(text, style = RefSectionLabelStyle, color = HomeColors.TextMuted)
    }
}

// --- Hero (greeting + location/profile/notification + wordmark + slogan) ----

/**
 * Replaces the previous flat [HomeGreetingRow]. Matches the reference's hero
 * composition — location row up top, ROJAN AI wordmark + the exact approved
 * slogan, a quiet radial glow accent standing in for the reference's portrait
 * photo (see file header doc comment: no real photo asset exists to use
 * honestly here) — on the same dark navy/deep-purple ground already used
 * app-wide, no new colours.
 */
@Composable
private fun HomeHeroCard(
    firstName: String,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = RefScreenMargin),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(HeroShape)
                .background(
                    ColorBrush.radialGradient(
                        colors = listOf(HomeColors.Glow.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(1f, 0f),
                        radius = 900f,
                    ),
                )
                .background(RefSurfaceFill)
                .border(1.dp, RefHairline, HeroShape)
                .padding(RojanDimens.SpaceMD),
        ) {
            Column {
                // Location + profile/notification row — spans the full hero
                // width regardless of the text/portrait split below it.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .rojanPressable(onClick = {}, role = Role.Button),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = RefAccent,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(RojanDimens.SpaceXS))
                        // Honest placeholder: no real geolocation/tenant-city source
                        // is wired into this screen — see file header doc comment.
                        Text(
                            "موقعیت من",
                            style = RojanTypography.Caption,
                            color = HomeColors.TextSecondary,
                        )
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = HomeColors.TextMuted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.width(RojanDimens.SpaceSM))
                    HomeIconChip(
                        icon = Icons.Outlined.Notifications,
                        contentDescription = "اعلان‌ها",
                        onClick = onNotificationsClick,
                    )
                    Spacer(Modifier.width(RojanDimens.SpaceSM))
                    HomeIconChip(
                        icon = Icons.Outlined.Person,
                        contentDescription = "پروفایل",
                        onClick = onProfileClick,
                    )
                }

                Spacer(Modifier.height(RojanDimens.SpaceLG))

                // Text block (start side) + portrait area (end side), matching
                // the reference's two-column hero split — see this file's
                // header doc comment for the portrait asset's provenance.
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "ROJAN AI",
                            style = RojanTypography.SectionTitle.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            ),
                            color = HomeColors.TextPrimary,
                        )
                        Spacer(Modifier.height(RojanDimens.SpaceXS))
                        // Approved slogan, exact text, as ONE logical string —
                        // no manual mid-sentence line break. A hard-coded `\n`
                        // inside an RTL-directed string previously rendered
                        // with the two halves visually out of order; letting
                        // Compose's own soft-wrap break the line (same as
                        // every other multi-word Persian string in this app)
                        // avoids that class of bug entirely.
                        Text(
                            "هوشمندتر مدیریت کن، زیباتر رشد کن",
                            style = RojanTypography.Body,
                            color = HomeColors.TextSecondary,
                        )
                        Spacer(Modifier.height(RojanDimens.SpaceMD))
                        Text(
                            "سلام $firstName جان، خوش برگشتی",
                            style = RojanTypography.Caption,
                            color = HomeColors.TextMuted,
                        )
                    }
                    Spacer(Modifier.width(RojanDimens.SpaceMD))
                    HomeHeroPortraitSlot()
                }
            }
        }
    }
}

/**
 * Reserved portrait area — see [HomeHeroCard]'s doc comment. Intentionally a
 * quiet radial glow on the existing dark ground, not a fabricated photo.
 *
 * `internal` (not `private`): reused as-is by [ai.rojan.designlab.screens.auth.AuthScreen]
 * for the same brand portrait on the Login/entry screen — same asset, same
 * treatment, no duplicated composable.
 */
@Composable
internal fun HomeHeroPortraitSlot() {
    val portraitShape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .size(width = 104.dp, height = 132.dp)
            .clip(portraitShape)
            .background(
                ColorBrush.radialGradient(
                    colors = listOf(HomeColors.Glow.copy(alpha = 0.35f), HomeColors.Magenta.copy(alpha = 0.12f)),
                ),
            )
            .border(1.dp, RefHairline, portraitShape),
    ) {
        Image(
            painter = painterResource(R.drawable.hero_customer_portrait),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun HomeIconChip(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(RojanDimens.MinTouchTarget)
            .clip(CircleShape)
            .background(RefSurfaceFill)
            .border(1.dp, RefHairline, CircleShape)
            .rojanPressable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = HomeColors.TextPrimary, modifier = Modifier.size(20.dp))
    }
}

// --- Search entry ---------------------------------------------------------

@Composable
private fun HomeSearchBar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    RefSurface(
        modifier = modifier.rojanPressable(onClick = onClick, role = Role.Button),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RojanDimens.SpaceSM, vertical = RojanDimens.SpaceSM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Reference shows a trailing filter affordance on the search bar —
            // decorative only (no separate filter feature exists to wire it
            // to), so it shares the same real onClick as the rest of the bar
            // rather than inventing new functionality.
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(RefAccent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = RefOnAccent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(RojanDimens.SpaceSM))
            Text(
                "جستجوی سالن، خدمت یا متخصص…",
                style = RojanTypography.Caption,
                color = HomeColors.TextMuted,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(RojanDimens.SpaceSM))
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = HomeColors.TextMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// --- Service category shortcuts --------------------------------------------

@Composable
private fun HomeCategoryRow(onCategoryClick: () -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = RefScreenMargin),
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        items(homeCategories) { category ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(64.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CategoryTileShape)
                        .background(RefSurfaceFill)
                        .border(1.dp, RefHairline, CategoryTileShape)
                        .rojanPressable(onClick = onCategoryClick, role = Role.Button),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        category.icon,
                        contentDescription = category.label,
                        tint = RefAccent,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.height(RojanDimens.SpaceXS))
                Text(
                    category.label,
                    style = RojanTypography.Caption,
                    color = HomeColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// --- Promotional banner (real bundled salon-interior photo) -----------------

@Composable
private fun HomePromoBanner(onPrimaryClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.9f)
            .clip(PromoShape)
            .border(1.dp, RefHairline, PromoShape),
    ) {
        Image(
            painter = painterResource(R.drawable.bg_master_luxury_salon),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    ColorBrush.horizontalGradient(
                        colors = listOf(
                            HomeColors.NavyBase.copy(alpha = 0.92f),
                            HomeColors.NavyBase.copy(alpha = 0.55f),
                            HomeColors.NavyBase.copy(alpha = 0.15f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "بهترین سالن‌های زیبایی نزدیک شما",
                style = RojanTypography.CardTitle.copy(fontWeight = FontWeight.Bold),
                color = HomeColors.TextPrimary,
            )
            Spacer(Modifier.height(RojanDimens.SpaceXS))
            Text(
                "کیفیت، زیبایی، اعتماد",
                style = RojanTypography.Caption,
                color = HomeColors.TextSecondary,
            )
            Spacer(Modifier.height(RojanDimens.SpaceMD))
            Box(
                // Reference shows a fully-rounded pill CTA here specifically
                // (distinct from the rectangular-radius buttons used
                // elsewhere in this screen) — RojanShapes.PremiumButton is
                // this design system's existing pill shape token, reused
                // as-is rather than a new local shape.
                modifier = Modifier
                    .clip(RojanShapes.PremiumButton)
                    .background(RefAccent)
                    .rojanPressable(onClick = onPrimaryClick, role = Role.Button)
                    .padding(horizontal = RojanDimens.SpaceLG, vertical = RojanDimens.SpaceSM),
            ) {
                Text("مشاهده سالن‌ها", style = RojanTypography.Button, color = RefOnAccent)
            }
        }
    }
}

// --- Suggested salons (real data only — no fabricated rating/distance) -----

@Composable
private fun HomeSalonSection(
    loading: Boolean,
    salons: List<Salon>,
    onSalonClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
) {
    Column {
        RefSectionLabel(
            "سالن‌های پیشنهادی",
            action = {
                Text(
                    "مشاهده همه",
                    style = RojanTypography.Caption,
                    color = RefAccent,
                    modifier = Modifier.rojanPressable(onClick = onViewAllClick, role = Role.Button),
                )
            },
        )
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
                    HomeSalonCard(salon, onClick = { onSalonClick(salon.id) })
                }
            }
        }
    }
}

@Composable
private fun HomeSalonCard(salon: Salon, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(220.dp)
            .clip(RefCardShape)
            .background(RefSurfaceFill)
            .border(1.dp, RefHairline, RefCardShape)
            .rojanPressable(onClick = onClick, role = Role.Button),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.6f)
                    .background(RefTileFill),
                contentAlignment = Alignment.Center,
            ) {
                RojanRemoteImage(
                    url = salon.logoUrl,
                    contentDescription = salon.name,
                    modifier = Modifier.fillMaxSize(),
                    fallback = {
                        Icon(
                            Icons.Outlined.Storefront,
                            contentDescription = null,
                            tint = HomeColors.TextSecondary,
                            modifier = Modifier.size(32.dp),
                        )
                    },
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(RojanDimens.SpaceSM)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(HomeColors.NavyBase.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.FavoriteBorder,
                        contentDescription = "علاقه‌مندی",
                        tint = HomeColors.TextPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Column(modifier = Modifier.padding(RojanDimens.SpaceMD), horizontalAlignment = Alignment.End) {
                Text(
                    salon.name,
                    style = RojanTypography.Body,
                    color = HomeColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
                Spacer(Modifier.height(RojanDimens.SpaceSM))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(RefButtonHeight.times(0.7f))
                        .clip(RoundedCornerShape(RefButtonRadius))
                        .background(RefAccent)
                        .rojanPressable(onClick = onClick, role = Role.Button),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("رزرو نوبت", style = RojanTypography.Caption.copy(fontWeight = FontWeight.SemiBold), color = RefOnAccent)
                }
            }
        }
    }
}

@Composable
private fun HomeSalonSkeleton() {
    Box(
        modifier = Modifier
            .width(220.dp)
            .height(220.dp)
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
