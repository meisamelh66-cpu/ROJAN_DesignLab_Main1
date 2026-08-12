package ai.rojan.designlab.screens.salon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.domain.repository.SalonWorkingHours
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.relationship.SalonRelationshipViewModel
import ai.rojan.designlab.presentation.relationship.SalonRelationshipViewModelFactory
import ai.rojan.designlab.presentation.salon.SalonDetailsViewModel
import ai.rojan.designlab.presentation.salon.SalonDetailsViewModelFactory
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.state.RojanErrorState
import ai.rojan.designlab.ui.components.state.RojanLoadingState
import ai.rojan.designlab.ui.theme.RojanAquaMint
import ai.rojan.designlab.ui.theme.RojanBlushPink
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanGradients
import ai.rojan.designlab.ui.theme.RojanPearlPink
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanSoftLavender
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.image.RojanRemoteImage
import ai.rojan.designlab.ui.components.image.SpecialistAvatar
import ai.rojan.designlab.ui.components.rtl.RtlInfoRow
import ai.rojan.designlab.ui.components.rtl.RtlListRow
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import java.util.Calendar

/** Hero rebuild: circular salon logo size — 64dp increased ~35%. */
private val LOGO_SIZE = 86.dp

/** Hero rebuild: hero image height. */
private val HERO_HEIGHT = 236.dp

/**
 * Hero rebuild: square top corners (the image is the literal top of the
 * page content, flush with the screen edge — not a floating card) and
 * rounded bottom corners (where it transitions into the page content) —
 * replaces [RojanShapes.GlassCard]'s all-four-corners rounding, which is
 * what made the hero read as a separate "card" sitting in the page
 * rather than a true hero banner.
 */
private val HERO_SHAPE = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = 0.dp,
    bottomStart = 32.dp,
    bottomEnd = 32.dp,
)

/** Deterministic per-salon/specialist tint, mirroring [ai.rojan.designlab.screens.booking.SalonListScreen]'s [colorSeedFor]-equivalent: neither the backend `Salon` nor `Specialist` has a color/branding or photo-URL concept this app can render (no image-loading library for remote URLs exists in this codebase), so this only varies an accent tint, never fabricates business data. */
private val accentPalette = listOf(RojanSoftLavender, RojanAquaMint, RojanBlushPink, RojanPearlPink)
private fun accentFor(id: String) = accentPalette[Math.floorMod(id.hashCode(), accentPalette.size)]

/** `java.time.DayOfWeek`'s English enum name, as returned by the backend (`WorkingHoursResponse.dayOfWeek`). */
private fun String.toPersianDayLabel(): String = when (this) {
    "SATURDAY" -> "شنبه"
    "SUNDAY" -> "یکشنبه"
    "MONDAY" -> "دوشنبه"
    "TUESDAY" -> "سه‌شنبه"
    "WEDNESDAY" -> "چهارشنبه"
    "THURSDAY" -> "پنجشنبه"
    "FRIDAY" -> "جمعه"
    else -> this
}

private fun currentBackendDayOfWeek(): String = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
    Calendar.SATURDAY -> "SATURDAY"
    Calendar.SUNDAY -> "SUNDAY"
    Calendar.MONDAY -> "MONDAY"
    Calendar.TUESDAY -> "TUESDAY"
    Calendar.WEDNESDAY -> "WEDNESDAY"
    Calendar.THURSDAY -> "THURSDAY"
    else -> "FRIDAY"
}

/** "HH:mm:ss", zero-padded to line up with the backend's own "09:00:00"-shaped interval strings so a plain lexical compare works. */
private fun currentTimeOfDayString(): String {
    val calendar = Calendar.getInstance()
    return "%02d:%02d:00".format(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
}

/**
 * Client-side "open now" computed from data this screen already fetched -
 * no new API call, no server-side open-now field exists on the backend.
 * `null` means today has no working-hours entry at all (unknown, not
 * "closed"); `false` means today has hours but the current time falls
 * outside every interval.
 */
private fun isOpenNow(workingHours: List<SalonWorkingHours>): Boolean? {
    val today = workingHours.find { it.dayOfWeek == currentBackendDayOfWeek() } ?: return null
    if (today.intervals.isEmpty()) return false
    val now = currentTimeOfDayString()
    return today.intervals.any { now >= it.start && now <= it.end }
}

/**
 * Journey 1, Screen 2: Salon Details.
 *
 * **Android <-> Backend Full Integration milestone:** now backed by
 * [SalonDetailsViewModel] -> `GET /api/v1/salons/{salonId}`,
 * `GET .../categories`, `GET .../categories/{categoryId}/services` (fanned
 * out per category — there is no salon-wide "all services" endpoint), and
 * `GET .../specialists`. Several sections that relied on
 * `ai.rojan.designlab.data.demo.DemoSalon`-only fields are gone rather than
 * faked:
 * - Rating/review count, phone-book-style facilities list, and a real photo
 *   gallery all depended on data the backend `Salon` doesn't have (no
 *   reviews system, no facilities modeling, no multi-photo gallery
 *   concept) — their sections are removed, not rendered empty or fabricated.
 * - The tagline row now shows the backend `Salon.description` (nullable —
 *   omitted when absent) instead of the demo's always-present tagline.
 * - Salon Discovery milestone: `Salon.logoUrl`/`Specialist.photoUrl` now
 *   render as real remote images via [RojanRemoteImage] (Coil, added that
 *   milestone) — the hero banner/circular logo and specialist avatars fall
 *   back to the existing tinted [RojanIconContainer]/[SpecialistAvatar] icon
 *   path only when the URL is null, blank, or fails to load.
 * - Salon Discovery milestone: an "باز / تعطیل" (open/closed) badge is
 *   computed client-side from the already-fetched [SalonWorkingHours] list
 *   (no extra API call — see [isOpenNow]) and shown next to the working
 *   hours section. Deliberately NOT shown on the salon list cards — that
 *   would require one working-hours fetch per card (N+1), whereas this
 *   screen already fetches hours for its one salon.
 * - "نظرات" (reviews) is removed entirely — no reviews API exists on the
 *   backend.
 *
 * Booking Experience Refactor, spec section 10: when reached from the
 * category-first flow, [selectedServiceIds] is non-null and [services] is
 * filtered to only those.
 *
 * [onContinueBooking], when provided, renders a bottom CTA implementing
 * "If only one specialist exists: Skip specialist selection completely" —
 * approximated as "only one specialist at this salon" (no
 * capability-to-service mapping exists in the data model), same disclosed
 * simplification as before this milestone.
 *
 * Production Data Integrity Phase 1: the "Follow" toggle was removed
 * (see the Coming-Soon-gating note further down) — no backend
 * favorite/follow endpoint exists.
 */
@Composable
fun SalonDetailsScreen(
    salonId: String,
    onBackClick: () -> Unit,
    onSpecialistClick: (String) -> Unit,
    onServiceClick: (String) -> Unit,
    onLoginRequired: () -> Unit = {},
    selectedServiceIds: List<String>? = null,
    onContinueBooking: ((autoSelectedSpecialistId: String?) -> Unit)? = null,
    viewModel: SalonDetailsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = run {
            val container = BackendApiContainerHolder.get(LocalContext.current)
            SalonDetailsViewModelFactory(
                salonId = salonId,
                salonRepository = container.salonRepository,
                serviceCategoryRepository = container.serviceCategoryRepository,
                serviceRepository = container.serviceRepository,
                specialistRepository = container.specialistRepository,
                workingHoursRepository = container.workingHoursRepository,
            )
        },
    ),
    relationshipViewModel: SalonRelationshipViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        key = "salon_relationship_$salonId",
        factory = run {
            val container = BackendApiContainerHolder.get(LocalContext.current)
            SalonRelationshipViewModelFactory(
                salonId = salonId,
                followSalonUseCase = ai.rojan.designlab.domain.usecase.relationship.FollowSalonUseCase(container.customerRelationshipRepository),
                unfollowSalonUseCase = ai.rojan.designlab.domain.usecase.relationship.UnfollowSalonUseCase(container.customerRelationshipRepository),
                getFollowedSalonsUseCase = ai.rojan.designlab.domain.usecase.relationship.GetFollowedSalonsUseCase(container.customerRelationshipRepository),
                favoriteSalonUseCase = ai.rojan.designlab.domain.usecase.relationship.FavoriteSalonUseCase(container.customerRelationshipRepository),
                unfavoriteSalonUseCase = ai.rojan.designlab.domain.usecase.relationship.UnfavoriteSalonUseCase(container.customerRelationshipRepository),
                getFavoriteSalonsUseCase = ai.rojan.designlab.domain.usecase.relationship.GetFavoriteSalonsUseCase(container.customerRelationshipRepository),
            )
        },
    ),
) {
    // Protected Route Handling fix: Follow/Favorite are the only actions on
    // this otherwise-unauthenticated-browsable screen that need a real
    // customer session. relationshipViewModel.requiresLogin flips true when
    // the backend rejects one with a real 401 - redirect to login rather
    // than leave it as an opaque error, then immediately consume the event
    // so it doesn't refire on the next recomposition.
    LaunchedEffect(relationshipViewModel.requiresLogin) {
        if (relationshipViewModel.requiresLogin) {
            onLoginRequired()
            relationshipViewModel.consumeLoginRequired()
        }
    }

    HomeBackgroundTheme {
        when (val loadState = viewModel.state) {
            is UiState.Loading -> SalonDetailsScaffoldState(onBackClick) {
                RojanLoadingState(message = "در حال بارگذاری سالن...")
            }
            is UiState.Empty -> SalonDetailsScaffoldState(onBackClick) {
                RojanErrorState(title = "سالن یافت نشد", actionLabel = "بازگشت", onAction = onBackClick)
            }
            is UiState.Error -> SalonDetailsScaffoldState(onBackClick) {
                RojanErrorState(description = loadState.message, actionLabel = "تلاش مجدد", onAction = viewModel::retry)
            }
            is UiState.Success -> {
                val context = LocalContext.current
                val data = loadState.data
                val salon = data.salon
                val specialists = data.specialists
                val services = if (selectedServiceIds != null) {
                    data.services.filter { it.id in selectedServiceIds }
                } else {
                    data.services
                }

                // Alignment fix: the hero must be genuinely edge-to-edge and
                // perfectly centered, which a "widen + negative-offset" layout
                // trick got wrong under RTL. The robust fix is structural: the
                // LazyColumn itself applies no horizontal margin, so the hero
                // is naturally full width with zero offset math. Every other
                // item opts into the normal side margin individually.
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = RojanDimens.SpaceMD),
                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
                ) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SalonHeroSection(salon = salon, onBackClick = onBackClick)

                            Spacer(modifier = Modifier.height(LOGO_SIZE / 2 + 4.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = RojanDimens.SpaceMD),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                            ) {
                                // Customer Relationship Foundation: real, backend-backed
                                // Follow/Favorite controls, replacing the Spacer this slot
                                // held during Production Data Integrity Phase 1 (when no
                                // backend endpoint existed for either yet).
                                RelationshipButtonsRow(relationshipViewModel)
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End,
                                ) {
                                    Text(
                                        salon.name,
                                        style = RojanTypography.HeroTitle.copy(fontSize = 28.sp, lineHeight = 34.sp),
                                        color = HomeColors.TextPrimary,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    salon.description?.let { description ->
                                        Text(
                                            description,
                                            style = RojanTypography.Body,
                                            color = HomeColors.TextSecondary,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                            }
                            relationshipViewModel.followError?.let { message ->
                                Text(
                                    message,
                                    style = RojanTypography.Caption,
                                    color = HomeColors.TextSecondary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceXS),
                                )
                            }
                        }
                    }

                    item {
                        HomeGlassSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RojanDimens.SpaceMD),
                            shape = RojanShapes.Small,
                            glassAlpha = 0.28f,
                            glassSecondaryAlpha = 0.10f,
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = RojanDimens.SpaceLG, vertical = RojanDimens.SpaceMD),
                                verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
                                horizontalAlignment = Alignment.End,
                            ) {
                                RtlInfoRow(
                                    Icons.Filled.LocationOn,
                                    salon.address,
                                    iconTint = HomeColors.TextSecondary,
                                    textColor = HomeColors.TextSecondary,
                                    modifier = Modifier.rojanPressable(
                                        onClick = {
                                            // Text-query navigation, not coordinate-based - the
                                            // backend Salon has no geo-coordinate concept, only a
                                            // postal address (see this screen's own doc comment on
                                            // real-data gaps). "geo:0,0?q=" is the standard Android
                                            // way to open Maps on a text query with no known point.
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(salon.address)))
                                            runCatching { context.startActivity(intent) }
                                        },
                                    ),
                                )
                                RtlInfoRow(
                                    Icons.Filled.Phone,
                                    salon.phone,
                                    iconTint = HomeColors.TextSecondary,
                                    textColor = HomeColors.TextSecondary,
                                    modifier = Modifier.rojanPressable(
                                        onClick = {
                                            // ACTION_DIAL (not ACTION_CALL): opens the dialer
                                            // pre-filled, doesn't place the call itself - needs no
                                            // CALL_PHONE runtime permission.
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${salon.phone}"))
                                            runCatching { context.startActivity(intent) }
                                        },
                                    ),
                                )
                            }
                        }
                    }

                    if (data.workingHours.isNotEmpty()) {
                        item { RtlSectionHeader("ساعات کاری", color = HomeColors.TextPrimary) }
                        item {
                            HomeGlassSurface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = RojanDimens.SpaceMD),
                                shape = RojanShapes.Small,
                                glassAlpha = 0.28f,
                                glassSecondaryAlpha = 0.10f,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = RojanDimens.SpaceLG, vertical = RojanDimens.SpaceMD),
                                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                                ) {
                                    val openNow = remember(data.workingHours) { isOpenNow(data.workingHours) }
                                    if (openNow != null) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(if (openNow) HomeColors.Glow else HomeColors.TextSecondary, CircleShape),
                                            )
                                            Spacer(modifier = Modifier.width(RojanDimens.SpaceXS))
                                            Text(
                                                if (openNow) "اکنون باز است" else "اکنون تعطیل است",
                                                style = RojanTypography.Caption,
                                                color = if (openNow) HomeColors.Glow else HomeColors.TextSecondary,
                                            )
                                        }
                                    }
                                    data.workingHours.forEach { hours ->
                                        RtlListRow(
                                            title = hours.dayOfWeek.toPersianDayLabel(),
                                            titleColor = HomeColors.TextPrimary,
                                            icon = Icons.Filled.AccessTime,
                                            iconTint = HomeColors.TextSecondary,
                                            value = hours.intervals.joinToString(" ، ") { "${it.start.take(5)}-${it.end.take(5)}" },
                                            valueColor = HomeColors.TextSecondary,
                                            valueStyle = RojanTypography.Caption,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (specialists.isNotEmpty()) {
                        item { RtlSectionHeader("متخصصان", color = HomeColors.TextPrimary) }
                        item {
                            LazyRow(
                                modifier = Modifier.padding(horizontal = RojanDimens.SpaceMD),
                                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                            ) {
                                itemsIndexed(specialists) { index, specialist ->
                                    HomeGlassSurface(
                                        modifier = Modifier
                                            .rojanEnterAnimation(delayMillis = index * 60)
                                            .rojanPressable(onClick = { onSpecialistClick(specialist.id) }),
                                        shape = RojanShapes.Small,
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .padding(RojanDimens.SpaceSM)
                                                .width(120.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .background(accentFor(specialist.id).copy(alpha = 0.5f), CircleShape),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                SpecialistAvatar(
                                                    assetRes = null,
                                                    contentDescription = specialist.displayName,
                                                    modifier = Modifier.fillMaxSize(),
                                                    photoUrl = specialist.photoUrl,
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(RojanDimens.SpaceXS))
                                            Text(
                                                specialist.displayName,
                                                style = RojanTypography.Caption,
                                                color = HomeColors.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (services.isNotEmpty()) {
                        item { RtlSectionHeader("خدمات", color = HomeColors.TextPrimary) }
                        itemsIndexed(services) { index, service ->
                            HomeGlassSurface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = RojanDimens.SpaceMD)
                                    .rojanEnterAnimation(delayMillis = index * 60)
                                    .rojanPressable(onClick = { onServiceClick(service.id) }),
                                shape = RojanShapes.Small,
                            ) {
                                RtlListRow(
                                    title = service.name,
                                    titleColor = HomeColors.TextPrimary,
                                    subtitle = "${service.durationMinutes} دقیقه",
                                    subtitleColor = HomeColors.TextSecondary,
                                    value = "${service.price.toInt()} تومان",
                                    valueColor = HomeColors.Glow,
                                    modifier = Modifier.padding(RojanDimens.SpaceMD),
                                )
                            }
                        }
                    }

                    if (onContinueBooking != null) {
                        item {
                            PremiumButton(
                                text = "ادامه رزرو",
                                onClick = {
                                    val autoSpecialistId = if (specialists.size == 1) specialists.first().id else null
                                    onContinueBooking(autoSpecialistId)
                                },
                                modifier = Modifier
                                    .padding(horizontal = RojanDimens.SpaceMD)
                                    .size(width = RojanDimens.ButtonWidth, height = RojanDimens.ButtonHeight),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Shared loading/error/not-found scaffold: a back button (so a stuck load/error never dead-ends the user) above a centered state card. */
@Composable
private fun SalonDetailsScaffoldState(onBackClick: () -> Unit, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.padding(RojanDimens.SpaceMD)) {
            GlassBackButton(onClick = onBackClick)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/**
 * Hero rebuild: a single Hero component owning the edge-to-edge banner, the
 * back button floating on top of it, the bottom gradient, and the circular
 * logo overlapping its bottom edge. Salon Discovery milestone: both the
 * banner and the circular logo render [salon.logoUrl] via [RojanRemoteImage]
 * when present, falling back to [RojanIconContainer]'s icon (tinted via
 * [accentFor]) when it's null or fails to load.
 */
@Composable
private fun SalonHeroSection(
    salon: Salon,
    onBackClick: () -> Unit,
) {
    val tint = remember(salon.id) { accentFor(salon.id) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HERO_HEIGHT)
                .background(tint.copy(alpha = 0.5f), HERO_SHAPE),
            contentAlignment = Alignment.Center,
        ) {
            RojanRemoteImage(
                url = salon.logoUrl,
                contentDescription = null,
                shape = HERO_SHAPE,
                modifier = Modifier.fillMaxSize(),
                fallback = {
                    RojanIconContainer(
                        imageVector = Icons.Filled.Storefront,
                        contentDescription = null,
                        tint = HomeColors.TextPrimary,
                        size = RojanIconSize.XLarge,
                    )
                },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .align(Alignment.BottomCenter)
                    .clip(HERO_SHAPE)
                    .background(brush = RojanGradients.ImageScrim)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(RojanDimens.SpaceMD),
            ) {
                GlassBackButton(onClick = onBackClick)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = LOGO_SIZE / 2)
                .size(LOGO_SIZE)
                .background(HomeColors.DeepPurple, CircleShape)
                .padding(4.dp)
                .background(tint.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            RojanRemoteImage(
                url = salon.logoUrl,
                contentDescription = salon.name,
                shape = CircleShape,
                modifier = Modifier.fillMaxSize(),
                fallback = { Icon(Icons.Filled.Storefront, contentDescription = null, tint = HomeColors.TextPrimary) },
            )
        }
    }
}

/**
 * Follow (bell - updates/news intent) and Favorite (heart - personal-bookmark
 * intent), kept visually and semantically distinct per the backend's own
 * deliberate separation of the two concepts. Both read/write through
 * [SalonRelationshipViewModel], never a fake local toggle.
 */
@Composable
private fun RelationshipButtonsRow(viewModel: SalonRelationshipViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS)) {
        RelationshipIconButton(
            icon = if (viewModel.isFollowing) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone,
            contentDescription = if (viewModel.isFollowing) "لغو دنبال کردن سالن" else "دنبال کردن سالن",
            tint = if (viewModel.isFollowing) HomeColors.Glow else HomeColors.TextSecondary,
            loading = viewModel.isInitialLoading || viewModel.isFollowActionInProgress,
            onClick = viewModel::toggleFollow,
        )
        RelationshipIconButton(
            icon = if (viewModel.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (viewModel.isFavorite) "حذف از علاقه‌مندی‌ها" else "افزودن به علاقه‌مندی‌ها",
            tint = if (viewModel.isFavorite) HomeColors.Glow else HomeColors.TextSecondary,
            loading = viewModel.isInitialLoading || viewModel.isFavoriteActionInProgress,
            onClick = viewModel::toggleFavorite,
        )
    }
}

@Composable
private fun RelationshipIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    loading: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(RojanDimens.MinTouchTarget)
            .rojanPressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = HomeColors.TextSecondary, strokeWidth = 2.dp)
        } else {
            RojanIconContainer(imageVector = icon, contentDescription = contentDescription, tint = tint, size = RojanIconSize.Medium)
        }
    }
}
