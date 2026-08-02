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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.presentation.salon.SalonDetailsViewModel
import ai.rojan.designlab.presentation.salon.SalonDetailsViewModelFactory
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.effects.RojanAmbientGlow
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
import ai.rojan.designlab.ui.components.image.SpecialistAvatar
import ai.rojan.designlab.ui.components.rtl.RtlInfoRow
import ai.rojan.designlab.ui.components.rtl.RtlListRow
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader

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
 * - Rating/review count, working hours, phone-book-style facilities list,
 *   and the photo gallery all depended on data the backend `Salon` doesn't
 *   have (no reviews system, no facilities/hours modeling, no image URLs
 *   yet) — their sections are removed, not rendered empty or fabricated.
 * - The tagline row now shows the backend `Salon.description` (nullable —
 *   omitted when absent) instead of the demo's always-present tagline.
 * - Specialist/salon photos: the backend's `Specialist.photoUrl` is a
 *   remote URL, and this app has no URL image-loading dependency (Coil et
 *   al.) — every avatar/hero renders through the existing icon-fallback
 *   path ([SpecialistAvatar]/[RojanIconContainer]) that already exists for
 *   "no local asset" rather than adding a new library mid-milestone.
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
 * UX Refactor Phase 1: "Follow (optional)" reuses the existing
 * [CustomerEcosystemViewModel] favorite system — untouched this milestone
 * (customer-ecosystem/local-state territory, out of this catalog swap's
 * scope), works pre-login same as before.
 */
@Composable
fun SalonDetailsScreen(
    salonId: String,
    ecosystemViewModel: CustomerEcosystemViewModel,
    onBackClick: () -> Unit,
    onSpecialistClick: (String) -> Unit,
    onServiceClick: (String) -> Unit,
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
            )
        },
    ),
) {
    val isFollowed = ecosystemViewModel.state.favoriteSalonIds.contains(salonId)

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
                                Box(contentAlignment = Alignment.Center) {
                                    if (isFollowed) {
                                        RojanAmbientGlow(
                                            modifier = Modifier.size(44.dp),
                                            color = HomeColors.Magenta,
                                            alpha = 0.30f,
                                            blurRadius = 10.dp,
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(RojanDimens.MinTouchTarget)
                                            .rojanPressable(onClick = { ecosystemViewModel.toggleFavoriteSalon(salonId) }),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = if (isFollowed) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                            contentDescription = if (isFollowed) "لغو دنبال کردن این سالن" else "دنبال کردن این سالن",
                                            tint = HomeColors.Magenta,
                                            modifier = Modifier.size(28.dp),
                                        )
                                    }
                                }
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
                                RtlInfoRow(Icons.Filled.LocationOn, salon.address, iconTint = HomeColors.TextSecondary, textColor = HomeColors.TextSecondary)
                                RtlInfoRow(Icons.Filled.Phone, salon.phone, iconTint = HomeColors.TextSecondary, textColor = HomeColors.TextSecondary)
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
 * logo overlapping its bottom edge. No photo path exists this milestone
 * (see [SalonDetailsScreen]'s doc comment) — both the banner and the logo
 * always render through [RojanIconContainer]'s icon fallback, tinted via
 * [accentFor] instead of a per-salon image.
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
            RojanIconContainer(
                imageVector = Icons.Filled.Storefront,
                contentDescription = null,
                tint = HomeColors.TextPrimary,
                size = RojanIconSize.XLarge,
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
            Icon(Icons.Filled.Storefront, contentDescription = null, tint = HomeColors.TextPrimary)
        }
    }
}
