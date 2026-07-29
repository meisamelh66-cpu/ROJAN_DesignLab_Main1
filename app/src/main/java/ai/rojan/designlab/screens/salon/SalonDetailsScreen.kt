package ai.rojan.designlab.screens.salon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import ai.rojan.designlab.data.demo.DemoSalon
import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.ui.animation.RojanAnimations
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.background.WarmBackground
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.effects.RojanAmbientGlow
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.state.RojanErrorState
import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanGradients
import ai.rojan.designlab.ui.theme.RojanRatingGold
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.theme.RojanVividMagenta
import ai.rojan.designlab.ui.theme.RojanWarmWhite
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.image.RojanSampleImage
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

/**
 * Journey 1, Screen 2: Salon Details.
 *
 * Booking Experience Refactor, spec section 10: when reached from the
 * new category-first flow, [selectedServiceIds] is non-null and
 * [services] is filtered to only those — "Only the services already
 * selected by the user. Do NOT display every service offered by the
 * salon." When `null` (the original Journey 1 browse-first entry
 * point), every salon service still shows, unchanged.
 *
 * [onContinueBooking], when provided, renders a bottom CTA implementing
 * "If only one specialist exists: Skip specialist selection completely"
 * — approximated here as "only one specialist at this salon" (spec asks
 * for "capable of performing the selected services" specifically, but
 * no capability-to-service mapping exists in the data model yet; this
 * is a disclosed simplification, not silently different behavior).
 *
 * If [salonId] doesn't resolve to a real demo entry (shouldn't happen
 * via normal navigation, but defensive nonetheless), shows a simple
 * "not found" state rather than crashing — no dead-end, per this
 * phase's "no empty pages" rule applied defensively.
 *
 * UX Refactor Phase 1: "Follow (optional)" on this screen reuses the
 * existing Favorite system ([CustomerEcosystemViewModel.toggleFavoriteSalon]
 * / [ai.rojan.designlab.domain.customer.CustomerEcosystemState.favoriteSalonIds])
 * rather than a new, separate concept — a confirmed scope decision, not
 * an oversight. Works pre-login: [ecosystemViewModel] is created
 * unconditionally at nav-graph scope, before any OTP authentication.
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
) {
    val catalogEngine = remember { CatalogEngine() }
    val salon = catalogEngine.findSalonById(salonId)
    val isFollowed = ecosystemViewModel.state.favoriteSalonIds.contains(salonId)

    WarmBackground {
        if (salon == null) {
            // Final Premium Polish, Phase 2: was a bare centered Text with no
            // icon/card/retry affordance — the exact gap RojanErrorState
            // (Phase 1) was built to close.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(RojanDimens.SpaceMD),
                contentAlignment = Alignment.Center,
            ) {
                RojanErrorState(
                    title = "سالن یافت نشد",
                    actionLabel = "بازگشت",
                    onAction = onBackClick,
                )
            }
            return@WarmBackground
        }

        val specialists = catalogEngine.specialistsForSalon(salonId)
        val allServices = catalogEngine.servicesForSalon(salonId)
        val services = if (selectedServiceIds != null) {
            allServices.filter { it.id in selectedServiceIds }
        } else {
            allServices
        }
        val reviews = catalogEngine.reviewsFor(salonId)

        // Alignment fix: the hero must be genuinely edge-to-edge and
        // perfectly centered, which a "widen + negative-offset" layout
        // trick got wrong under RTL (Compose's automatic RTL mirroring
        // for a child whose declared size equals its parent's produces a
        // lopsided result, not a symmetric one). The robust fix is
        // structural, not a cleverer offset: the LazyColumn itself no
        // longer applies horizontal margin, so the hero is naturally
        // full width with zero offset math. Every other item opts into
        // the normal side margin individually instead.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item {
                // Hero rebuild: one Hero component owns the image, the
                // back button floating on it, the bottom gradient, and
                // the overlapping circular logo — back button is no
                // longer a separate preceding row, so the hero image is
                // the literal top of the page content, not a card sitting
                // below a toolbar. Title/subtitle follow directly after,
                // with just enough clearance to sit below the logo.
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
                        // RTL fix: favorite icon now leads (renders at the
                        // left) and the title/tagline column trails,
                        // pinned to the right — salon name is the primary
                        // information here and reads right-anchored, per
                        // the RTL rule "titles/salon information start
                        // from the right side". Row order swapped only for
                        // this pair; no LocalLayoutDirection change.
                        Box(contentAlignment = Alignment.Center) {
                            // Premium Effects: a quiet glow appears only once
                            // the salon is actually followed — an "active
                            // element" cue, not a permanent decoration on an
                            // icon that's off most of the time.
                            if (isFollowed) {
                                RojanAmbientGlow(
                                    modifier = Modifier.size(44.dp),
                                    color = RojanVividMagenta,
                                    alpha = 0.30f,
                                    blurRadius = 10.dp,
                                )
                            }
                            // Phase 3 internal audit (Accessibility): the tap
                            // target is now a full 48dp box
                            // (RojanDimens.MinTouchTarget) — previously the
                            // 28dp icon itself was the only tappable area,
                            // under the app's own 48dp minimum (see
                            // GlassBackButton's sizing). The icon's visual
                            // size is unchanged; only the invisible tappable
                            // margin around it grew.
                            Box(
                                modifier = Modifier
                                    .size(RojanDimens.MinTouchTarget)
                                    .rojanPressable(onClick = { ecosystemViewModel.toggleFavoriteSalon(salonId) }),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (isFollowed) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = if (isFollowed) "لغو دنبال کردن این سالن" else "دنبال کردن این سالن",
                                    tint = RojanVividMagenta,
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
                                color = RojanTextOnGlass,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                salon.tagline,
                                style = RojanTypography.Body,
                                color = RojanTextSecondary,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            item {
                // UI Polish Pass: lower glassAlpha/glassSecondaryAlpha
                // than GlassSurface's default (0.40f/0.14f) for a softer
                // frost on this specific info card — an explicit local
                // choice via GlassSurface's own optional parameters, not
                // a change to the shared component or any other call site.
                GlassSurface(
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
                        // RTL fix: anchors each info row to the right edge
                        // of the card instead of the left, so the
                        // text-then-icon rows read as right-anchored
                        // content, not a left-hugging block.
                        horizontalAlignment = Alignment.End,
                    ) {
                        RtlInfoRow(Icons.Filled.Star, "${salon.rating} (${salon.reviewCount} نظر)", emphasize = true)
                        RtlInfoRow(Icons.Filled.LocationOn, salon.address)
                        RtlInfoRow(Icons.Filled.AccessTime, salon.workingHours)
                        RtlInfoRow(Icons.Filled.Phone, salon.phone)
                    }
                }
            }

            item { RtlSectionHeader("گالری تصاویر") }
            item {
                // UI Polish Pass: this app has exactly one real photo per
                // demo salon (no per-salon photo sets), so a 3-thumbnail
                // gallery row repeats that same photo — same image asset,
                // not fabricated content — purely to fill the approved
                // gallery UI pattern with equal-size, smoothly scrollable
                // thumbnails. Swaps to real distinct photos with zero
                // layout change whenever more than one photo per salon
                // exists.
                val galleryImages = salon.assetRes?.let { List(3) { _ -> it } } ?: emptyList()
                if (galleryImages.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.padding(horizontal = RojanDimens.SpaceMD),
                        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                    ) {
                        itemsIndexed(galleryImages) { index, resId ->
                            RojanSampleImage(
                                resId = resId,
                                contentDescription = salon.name,
                                modifier = Modifier
                                    .size(width = 140.dp, height = 100.dp)
                                    .rojanEnterAnimation(delayMillis = index * 60),
                                shape = RojanShapes.Small,
                            )
                        }
                    }
                }
            }

            item { RtlSectionHeader("امکانات") }
            item {
                // Layout Polish fix: a plain (non-wrapping) Row let long
                // facility lists overflow past the screen edge, squeezing
                // the last chip's Text into a near-zero-width column where
                // Compose wraps it one character per line. FlowRow wraps
                // chips onto additional rows instead, so every chip stays
                // on one line.
                FlowRow(
                    modifier = Modifier.padding(horizontal = RojanDimens.SpaceMD),
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                ) {
                    salon.facilities.forEach { facility ->
                        GlassSurface(shape = RojanShapes.Small) {
                            Text(
                                text = facility,
                                style = RojanTypography.Caption,
                                color = RojanTextPrimary,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = RojanDimens.SpaceSM, vertical = RojanDimens.SpaceXS),
                            )
                        }
                    }
                }
            }

            if (specialists.isNotEmpty()) {
                item { RtlSectionHeader("متخصصان") }
                item {
                    LazyRow(
                        modifier = Modifier.padding(horizontal = RojanDimens.SpaceMD),
                        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                    ) {
                        itemsIndexed(specialists) { index, specialist ->
                            GlassSurface(
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
                                            .background(specialist.colorSeed.copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        SpecialistAvatar(
                                            assetRes = specialist.assetRes,
                                            contentDescription = specialist.name,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(RojanDimens.SpaceXS))
                                    Text(
                                        specialist.name,
                                        style = RojanTypography.Caption,
                                        color = RojanTextPrimary,
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
                item { RtlSectionHeader("خدمات") }
                itemsIndexed(services) { index, service ->
                    GlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RojanDimens.SpaceMD)
                            .rojanEnterAnimation(delayMillis = index * 60)
                            .rojanPressable(onClick = { onServiceClick(service.id) }),
                        shape = RojanShapes.Small,
                    ) {
                        RtlListRow(
                            title = service.name,
                            subtitle = "${service.durationMinutes} دقیقه",
                            value = "${(service.discountPrice ?: service.price)} تومان",
                            valueColor = RojanAIGlow,
                            modifier = Modifier.padding(RojanDimens.SpaceMD),
                        )
                    }
                }
            }

            item { RtlSectionHeader("نظرات") }
            itemsIndexed(reviews) { index, review ->
                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RojanDimens.SpaceMD)
                        .rojanEnterAnimation(delayMillis = index * 60),
                    shape = RojanShapes.Small,
                ) {
                    Column(modifier = Modifier.padding(RojanDimens.SpaceMD)) {
                        RtlListRow(
                            title = review.authorName,
                            titleStyle = RojanTypography.Caption,
                            trailing = {
                                Icon(Icons.Filled.Star, null, tint = RojanRatingGold, modifier = Modifier.size(RojanDimens.IconSizeSmall))
                                Text(" ${review.rating}", style = RojanTypography.Caption, color = RojanTextSecondary)
                            },
                        )
                        Text(review.comment, style = RojanTypography.Caption, color = RojanTextSecondary)
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

/**
 * Hero rebuild: a single Hero component owning everything that reads as
 * "the header" — the edge-to-edge image, the back button floating on
 * top of it, the bottom gradient, and the circular logo overlapping its
 * bottom edge. The image is the parent container here: the back button
 * and logo are both children positioned relative to it (not siblings
 * placed above/below a separately-shaped "card"), and the image
 * continues underneath the logo rather than stopping short of it.
 */
@Composable
private fun SalonHeroSection(
    salon: DemoSalon,
    onBackClick: () -> Unit,
) {
    // Final Premium Polish, Phase 2: a one-time, subtle scale-in
    // (1.06f -> 1f) as the hero image first appears — the "smooth image
    // zoom" / "hero image transitions" touch from the Image Experience
    // spec, kept deliberately safe (a scroll-linked parallax was
    // considered and intentionally not attempted here: this Image
    // renders via ContentScale.Crop with zero overflow margin baked in,
    // so a translation tied to live scroll offset risks revealing a bare
    // edge at the top/bottom of the hero the moment scroll direction
    // reverses — a load-time-only scale carries none of that risk since
    // it's already at rest, matching CardWidthStandard etc, before the
    // user can scroll at all).
    var heroLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { heroLoaded = true }
    val heroScale by animateFloatAsState(
        targetValue = if (heroLoaded) 1f else 1.06f,
        animationSpec = RojanAnimations.ContentEnterSpec,
        label = "salon_hero_scale",
    )

    // Alignment fix: the hero is genuinely full width here (the caller's
    // LazyColumn no longer applies horizontal margin), so it's simply
    // fillMaxWidth() — no bleed/offset math needed, and therefore no way
    // for it to end up off-center.
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HERO_HEIGHT)
                .background(salon.colorSeed.copy(alpha = 0.5f), HERO_SHAPE),
            contentAlignment = Alignment.Center,
        ) {
            if (salon.assetRes != null) {
                RojanSampleImage(
                    resId = salon.assetRes,
                    contentDescription = salon.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(heroScale),
                    shape = HERO_SHAPE,
                )
            } else {
                RojanIconContainer(
                    imageVector = Icons.Filled.Storefront,
                    contentDescription = null,
                    tint = RojanTextOnGlass,
                    size = RojanIconSize.XLarge,
                )
            }

            // Subtle bottom shadow/gradient for a smoother transition
            // from photo into the page content below, rather than a
            // hard photo edge. Final Premium Polish, Phase 1: sourced
            // from RojanGradients.ImageScrim (same 0.22f alpha value,
            // now the single shared token) instead of a private inline
            // Brush literal only this screen could reuse.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .align(Alignment.BottomCenter)
                    .clip(HERO_SHAPE)
                    .background(brush = RojanGradients.ImageScrim)
            )

            // Back button floats directly on the image — the hero is
            // the top of the page content, not a card below a toolbar.
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(RojanDimens.SpaceMD),
            ) {
                GlassBackButton(onClick = onBackClick)
            }
        }

        // Circular salon logo, centered on the border between the hero
        // image and the page content below (half above, half below the
        // image's bottom edge) — same photo as the hero (this app has
        // one image per demo salon, no separate logo asset), with a
        // Warm White ring so it reads as a distinct badge over the photo
        // edge, and the image visibly continuing behind it.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = LOGO_SIZE / 2)
                .size(LOGO_SIZE)
                .background(RojanWarmWhite, CircleShape)
                .padding(4.dp)
                .background(salon.colorSeed.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (salon.assetRes != null) {
                RojanSampleImage(
                    resId = salon.assetRes,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                )
            } else {
                Icon(Icons.Filled.Storefront, contentDescription = null, tint = RojanTextOnGlass)
            }
        }
    }
}

