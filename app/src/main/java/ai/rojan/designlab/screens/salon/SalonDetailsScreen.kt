package ai.rojan.designlab.screens.salon

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.domain.repository.SalonWorkingHours
import ai.rojan.designlab.domain.repository.Service
import ai.rojan.designlab.domain.repository.Specialist
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.relationship.SalonRelationshipViewModel
import ai.rojan.designlab.presentation.relationship.SalonRelationshipViewModelFactory
import ai.rojan.designlab.presentation.salon.SalonDetailsViewModel
import ai.rojan.designlab.presentation.salon.SalonDetailsViewModelFactory
import ai.rojan.designlab.screens.customer.components.CustomerAccent as RefAccent
import ai.rojan.designlab.screens.customer.components.CustomerButtonHeight as RefButtonHeight
import ai.rojan.designlab.screens.customer.components.CustomerButtonRadius as RefButtonRadius
import ai.rojan.designlab.screens.customer.components.CustomerCardRadius as RefCardRadius
import ai.rojan.designlab.screens.customer.components.CustomerCardShape as RefCardShape
import ai.rojan.designlab.screens.customer.components.CustomerDivider as RefDivider
import ai.rojan.designlab.screens.customer.components.CustomerHairline as RefHairline
import ai.rojan.designlab.screens.customer.components.CustomerOnAccent as RefOnAccent
import ai.rojan.designlab.screens.customer.components.CustomerScreenMargin as RefScreenMargin
import ai.rojan.designlab.screens.customer.components.CustomerSectionLabelStyle as RefSectionLabelStyle
import ai.rojan.designlab.screens.customer.components.CustomerSurfaceFill as RefSurfaceFill
import ai.rojan.designlab.screens.customer.components.CustomerTopBarHeight as RefTopBarHeight
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.components.image.RojanRemoteImage
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography
import java.util.Calendar

/* =============================================================================
 * ROJAN Customer — REFERENCE SCREEN: Salon Detail
 *
 * Approved visual reference for the "quiet luxury" direction
 * (docs/design-review/customer/REFERENCE-SPEC-salon-detail.md).
 *
 * Everything visual is screen-local and `private`: this file changes NO shared
 * component, NO design-system token, NO colour, NO icon set, NO ViewModel, and
 * NO navigation. The public [SalonDetailsScreen] signature, its two
 * ViewModels, and their `RojanNavGraph` wiring are byte-identical to before.
 *
 * Removed from the previous implementation: corner sparkles, glowing gold
 * metallic borders (`HomeGlassSurface`), the circular glass "orb" back button,
 * 32dp card radius, the gradient-pill CTA (`PremiumButton`), the tinted hero
 * colour band + image scrim + overlapping-offset logo, filled icons, and the
 * violet `HomeColors.Glow` accent.
 *
 * Kept: ROJAN identity, the dark navy ground (`HomeBackgroundTheme`), rose gold
 * (`RojanPremiumBorderRoseGold` #E0A67A) as the single accent, glass only as a
 * ~4.5% translucent lift (no border, no glow), RTL, and every backend field.
 * ========================================================================== */

// Phase 4 (P1) token consolidation: RefScreenMargin / RefCardRadius /
// RefButtonRadius / RefButtonHeight / RefTopBarHeight / RefAccent / RefOnAccent
// / RefSurfaceFill / RefHairline / RefDivider / RefCardShape /
// RefSectionLabelStyle were literal duplicates of the Customer* design tokens
// and are now import aliases of those (see imports above) — the values live
// only in CustomerRefComponents. The four below are specific to this reference
// screen (logo/avatar scale, the 26sp salon-name display, the price caption)
// and stay local.
private val RefLogoSize = 72.dp
private val RefAvatarSize = 64.dp
private val RefSalonNameStyle = RojanTypography.Display.copy(fontSize = 26.sp, lineHeight = 34.sp)
private val RefPriceStyle = RojanTypography.Caption.copy(fontWeight = FontWeight.SemiBold)

// --- Backend-day / open-now helpers (unchanged behaviour) --------------------

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

private fun currentTimeOfDayString(): String {
    val c = Calendar.getInstance()
    return "%02d:%02d:00".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
}

/** `null` = today has no hours entry (unknown, not "closed"). */
private fun isOpenNow(workingHours: List<SalonWorkingHours>): Boolean? {
    val today = workingHours.find { it.dayOfWeek == currentBackendDayOfWeek() } ?: return null
    if (today.intervals.isEmpty()) return false
    val now = currentTimeOfDayString()
    return today.intervals.any { now >= it.start && now <= it.end }
}

/** Short "open until HH:MM" / "closed now" line for the header meta row, or null when unknown. */
private fun openStatusLabel(workingHours: List<SalonWorkingHours>): Pair<String, Boolean>? {
    val open = isOpenNow(workingHours) ?: return null
    val today = workingHours.find { it.dayOfWeek == currentBackendDayOfWeek() }
    val lastEnd = today?.intervals?.maxByOrNull { it.end }?.end?.take(5)
    return if (open && lastEnd != null) "باز تا $lastEnd" to true
    else if (open) "اکنون باز است" to true
    else "اکنون تعطیل است" to false
}

private fun String.initial(): String = trim().firstOrNull()?.toString() ?: "?"

// --- Screen -----------------------------------------------------------------

/**
 * Journey 1, Screen 2: Salon Details. Backed by [SalonDetailsViewModel]
 * (`GET /api/v1/salons/{id}` + categories + services + specialists +
 * working-hours) and [SalonRelationshipViewModel] (follow / favourite).
 *
 * Sections whose data the backend doesn't model (ratings/reviews,
 * facilities, photo gallery) are absent by design — never faked or rendered
 * empty. `selectedServiceIds` (category-first flow) filters services;
 * `onContinueBooking` (only when the caller provides it) renders the bottom
 * CTA and auto-skips specialist selection when the salon has exactly one.
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
    // Guest Salon Detail fix: only non-null when reached from a guest-visible
    // list (see RojanNavGraph's salon-tap call sites) — same slug the salon's
    // own PublicSalonRepository-sourced Salon.slug already carried.
    slug: String? = null,
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
                slug = slug,
                publicSalonRepository = container.publicSalonRepository,
                hasSession = { container.tokenRepository.accessToken()?.isNotBlank() == true },
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
    LaunchedEffect(relationshipViewModel.requiresLogin) {
        if (relationshipViewModel.requiresLogin) {
            onLoginRequired()
            relationshipViewModel.consumeLoginRequired()
        }
    }

    val loadState = viewModel.state

    HomeBackgroundTheme {
        Column(modifier = Modifier.fillMaxSize()) {

            RefTopBar(
                title = if (loadState is UiState.Success) loadState.data.salon.name else null,
                onBackClick = onBackClick,
                relationship = relationshipViewModel,
            )

            when (loadState) {
                is UiState.Loading -> RefLoadingSkeleton()

                is UiState.Empty -> RefCenteredState(
                    icon = Icons.Outlined.SearchOff,
                    title = "سالن یافت نشد",
                    body = "این سالن در دسترس نیست.",
                    actionLabel = "بازگشت",
                    onAction = onBackClick,
                )

                is UiState.Error -> RefCenteredState(
                    icon = Icons.Outlined.CloudOff,
                    title = "مشکلی پیش آمد",
                    body = loadState.message,
                    actionLabel = "تلاش مجدد",
                    onAction = viewModel::retry,
                )

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

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            top = RojanDimens.SpaceLG,
                            bottom = RojanDimens.SpaceXXL,
                        ),
                    ) {
                        item {
                            RefHeader(
                                salon = salon,
                                workingHours = data.workingHours,
                                onAddressClick = {
                                    val i = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(salon.address)))
                                    runCatching { context.startActivity(i) }
                                },
                            )
                        }

                        if (onContinueBooking != null) {
                            item {
                                RefPrimaryButton(
                                    label = "ادامه رزرو",
                                    onClick = {
                                        onContinueBooking(if (specialists.size == 1) specialists.first().id else null)
                                    },
                                    modifier = Modifier.padding(
                                        start = RefScreenMargin,
                                        end = RefScreenMargin,
                                        top = RojanDimens.SpaceLG,
                                        bottom = RojanDimens.SpaceXL,
                                    ),
                                )
                            }
                        } else {
                            item { Spacer(Modifier.height(RojanDimens.SpaceXL)) }
                        }

                        if (services.isNotEmpty()) {
                            item {
                                RefSectionLabel("خدمات")
                                if (onContinueBooking == null) {
                                    Text(
                                        "برای رزرو، خدمت مورد نظر را انتخاب کنید",
                                        style = RojanTypography.Caption,
                                        color = HomeColors.TextMuted,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = RefScreenMargin, vertical = RojanDimens.SpaceXS),
                                    )
                                }
                            }
                            item {
                                RefSurface(Modifier.padding(horizontal = RefScreenMargin)) {
                                    Column(Modifier.fillMaxWidth()) {
                                        services.forEachIndexed { index, service ->
                                            if (index > 0) RefRowDivider()
                                            RefServiceRow(service) { onServiceClick(service.id) }
                                        }
                                    }
                                }
                            }
                        }

                        if (specialists.isNotEmpty()) {
                            item { RefSectionSpacer(); RefSectionLabel("متخصصان") }
                            item {
                                LazyRow(
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = RefScreenMargin),
                                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
                                    modifier = Modifier.padding(top = RojanDimens.SpaceSM),
                                ) {
                                    items(specialists, key = { it.id }) { specialist ->
                                        RefSpecialistItem(specialist) { onSpecialistClick(specialist.id) }
                                    }
                                }
                            }
                        }

                        if (data.workingHours.isNotEmpty()) {
                            item { RefSectionSpacer(); RefSectionLabel("ساعات کاری") }
                            item {
                                RefHoursCard(
                                    workingHours = data.workingHours,
                                    modifier = Modifier.padding(horizontal = RefScreenMargin, vertical = RojanDimens.SpaceSM),
                                )
                            }
                        }

                        item { RefSectionSpacer(); RefSectionLabel("تماس") }
                        item {
                            RefSurface(Modifier.padding(horizontal = RefScreenMargin, vertical = RojanDimens.SpaceSM)) {
                                RefContactRow(phone = salon.phone) {
                                    val i = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${salon.phone}"))
                                    runCatching { context.startActivity(i) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Top bar ---------------------------------------------------------------

@Composable
private fun RefTopBar(
    title: String?,
    onBackClick: () -> Unit,
    relationship: SalonRelationshipViewModel,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(RefTopBarHeight)
                .padding(horizontal = RojanDimens.SpaceSM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RefIconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "بازگشت",
                tint = HomeColors.TextPrimary,
                onClick = onBackClick,
            )

            Text(
                title.orEmpty(),
                style = RojanTypography.Body,
                color = HomeColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = RojanDimens.SpaceXS),
            )

            RefRelationshipIcon(
                active = relationship.isFavorite,
                loading = relationship.isInitialLoading || relationship.isFavoriteActionInProgress,
                activeIcon = Icons.Outlined.Favorite,
                inactiveIcon = Icons.Outlined.FavoriteBorder,
                contentDescription = if (relationship.isFavorite) "حذف از علاقه‌مندی‌ها" else "افزودن به علاقه‌مندی‌ها",
                onClick = relationship::toggleFavorite,
            )
            RefRelationshipIcon(
                active = relationship.isFollowing,
                loading = relationship.isInitialLoading || relationship.isFollowActionInProgress,
                activeIcon = Icons.Outlined.NotificationsActive,
                inactiveIcon = Icons.Outlined.NotificationsNone,
                contentDescription = if (relationship.isFollowing) "لغو دنبال کردن سالن" else "دنبال کردن سالن",
                onClick = relationship::toggleFollow,
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(RefHairline),
        )

        relationship.followError?.let { message ->
            Text(
                message,
                style = RojanTypography.Caption,
                color = HomeColors.TextMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RefScreenMargin, vertical = RojanDimens.SpaceXS),
            )
        }
    }
}

@Composable
private fun RefRelationshipIcon(
    active: Boolean,
    loading: Boolean,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(RojanDimens.MinTouchTarget)
            .rojanPressable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = HomeColors.TextMuted,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                imageVector = if (active) activeIcon else inactiveIcon,
                contentDescription = contentDescription,
                tint = if (active) RefAccent else HomeColors.TextMuted,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun RefIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(RojanDimens.MinTouchTarget)
            .rojanPressable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(24.dp))
    }
}

// --- Header --------------------------------------------------------------

@Composable
private fun RefHeader(
    salon: Salon,
    workingHours: List<SalonWorkingHours>,
    onAddressClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RefScreenMargin),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(RefLogoSize)
                .clip(CircleShape)
                .background(RefSurfaceFill)
                .border(1.dp, RefHairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            RojanRemoteImage(
                url = salon.logoUrl,
                contentDescription = salon.name,
                shape = CircleShape,
                modifier = Modifier.fillMaxSize(),
                fallback = {
                    Text(
                        salon.name.initial(),
                        style = RojanTypography.Display.copy(fontSize = 24.sp),
                        color = RefAccent,
                    )
                },
            )
        }

        Spacer(Modifier.height(RojanDimens.SpaceMD))

        Text(
            salon.name,
            style = RefSalonNameStyle,
            color = HomeColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        salon.description?.takeIf { it.isNotBlank() }?.let { description ->
            Spacer(Modifier.height(RojanDimens.SpaceXS))
            Text(
                description,
                style = RojanTypography.Body,
                color = HomeColors.TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(RojanDimens.SpaceSM))
        RefMetaRow(
            address = salon.address,
            openStatus = remember(workingHours) { openStatusLabel(workingHours) },
            onAddressClick = onAddressClick,
        )
    }
}

@Composable
private fun RefMetaRow(
    address: String,
    openStatus: Pair<String, Boolean>?,
    onAddressClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Place,
            contentDescription = null,
            tint = HomeColors.TextMuted,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(RojanDimens.SpaceXS))
        Text(
            address,
            style = RojanTypography.Caption,
            color = HomeColors.TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .widthIn(max = 200.dp)
                .rojanPressable(onClick = onAddressClick),
        )
        openStatus?.let { (label, open) ->
            Text(
                "  ·  ",
                style = RojanTypography.Caption,
                color = HomeColors.TextMuted,
            )
            Icon(
                Icons.Outlined.Schedule,
                contentDescription = null,
                tint = if (open) RefAccent else HomeColors.TextMuted,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(RojanDimens.SpaceXS))
            Text(
                label,
                style = RojanTypography.Caption,
                color = if (open) RefAccent else HomeColors.TextMuted,
            )
        }
    }
}

// --- Primary CTA (solid rose gold, no gradient) --------------------------

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

// --- Section label + spacing -------------------------------------------

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

@Composable
private fun RefSectionSpacer() = Spacer(Modifier.height(RojanDimens.SpaceXL))

@Composable
private fun RefRowDivider() = Box(
    Modifier
        .fillMaxWidth()
        .padding(horizontal = RojanDimens.SpaceMD)
        .height(1.dp)
        .background(RefDivider),
)

// --- Flat surface (glass only as a whisper of translucent lift) --------

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

// --- Service row -------------------------------------------------------

@Composable
private fun RefServiceRow(service: Service, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .rojanPressable(onClick = onClick, role = Role.Button)
            .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceMD),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
            contentDescription = null,
            tint = HomeColors.TextMuted,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(RojanDimens.SpaceSM))
        Text(
            "${service.price.toInt()} تومان",
            style = RefPriceStyle,
            color = RefAccent,
        )
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(service.name, style = RojanTypography.Body, color = HomeColors.TextPrimary)
            Text(
                "${service.durationMinutes} دقیقه",
                style = RojanTypography.Caption,
                color = HomeColors.TextMuted,
            )
        }
    }
}

// --- Specialist item (no card) ---------------------------------------

@Composable
private fun RefSpecialistItem(specialist: Specialist, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(84.dp)
            .rojanPressable(onClick = onClick, role = Role.Button),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(RefAvatarSize)
                .clip(CircleShape)
                .background(RefSurfaceFill)
                .border(1.dp, RefHairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            RojanRemoteImage(
                url = specialist.photoUrl,
                contentDescription = specialist.displayName,
                shape = CircleShape,
                modifier = Modifier.fillMaxSize(),
                fallback = {
                    Text(
                        specialist.displayName.initial(),
                        style = RojanTypography.CardTitle,
                        color = RefAccent,
                    )
                },
            )
        }
        Spacer(Modifier.height(RojanDimens.SpaceXS))
        Text(
            specialist.displayName,
            style = RojanTypography.Caption,
            color = HomeColors.TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// --- Working hours card --------------------------------------------

@Composable
private fun RefHoursCard(
    workingHours: List<SalonWorkingHours>,
    modifier: Modifier = Modifier,
) {
    val today = remember { currentBackendDayOfWeek() }
    RefSurface(modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceSM),
        ) {
            workingHours.forEachIndexed { index, hours ->
                if (index > 0) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(RefDivider),
                    )
                }
                val isToday = hours.dayOfWeek == today
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = RojanDimens.SpaceSM),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        hours.intervals.joinToString(" ، ") { "${it.start.take(5)}-${it.end.take(5)}" }
                            .ifBlank { "تعطیل" },
                        style = RojanTypography.Caption,
                        color = HomeColors.TextMuted,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        hours.dayOfWeek.toPersianDayLabel(),
                        style = RojanTypography.Body,
                        color = if (isToday) HomeColors.TextPrimary else HomeColors.TextSecondary,
                    )
                    if (isToday) {
                        Spacer(Modifier.width(RojanDimens.SpaceSM))
                        Box(
                            Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(RefAccent),
                        )
                    }
                }
            }
        }
    }
}

// --- Contact row -------------------------------------------------

@Composable
private fun RefContactRow(phone: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .rojanPressable(onClick = onClick, role = Role.Button)
            .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceMD),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text(phone, style = RojanTypography.Body, color = HomeColors.TextPrimary)
        Spacer(Modifier.width(RojanDimens.SpaceSM))
        Icon(
            Icons.Outlined.Phone,
            contentDescription = "تماس با سالن",
            tint = RefAccent,
            modifier = Modifier.size(20.dp),
        )
    }
}

// --- Loading + centred state ------------------------------------

@Composable
private fun RefLoadingSkeleton() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "pulse",
    )

    @Composable
    fun bar(width: Modifier, height: Int) = Box(
        width
            .height(height.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(RefSurfaceFill)
            .alpha(pulse),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = RefScreenMargin)
            .padding(top = RojanDimens.SpaceLG),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(RefLogoSize)
                .clip(CircleShape)
                .background(RefSurfaceFill)
                .alpha(pulse),
        )
        Spacer(Modifier.height(RojanDimens.SpaceMD))
        bar(Modifier.fillMaxWidth(0.6f), 26)
        Spacer(Modifier.height(RojanDimens.SpaceSM))
        bar(Modifier.fillMaxWidth(0.8f), 16)
        Spacer(Modifier.height(RojanDimens.SpaceXL))
        repeat(3) {
            bar(Modifier.fillMaxWidth(0.35f), 14)
            Spacer(Modifier.height(RojanDimens.SpaceSM))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RefCardShape)
                    .background(RefSurfaceFill)
                    .alpha(pulse),
            )
            Spacer(Modifier.height(RojanDimens.SpaceXL))
        }
    }
}

@Composable
private fun RefCenteredState(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = RefScreenMargin),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = HomeColors.TextMuted, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(RojanDimens.SpaceMD))
        Text(title, style = RojanTypography.CardTitle, color = HomeColors.TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(RojanDimens.SpaceXS))
        Text(
            body,
            style = RojanTypography.Caption,
            color = HomeColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(RojanDimens.SpaceLG))
        RefPrimaryButton(
            label = actionLabel,
            onClick = onAction,
            modifier = Modifier.widthIn(max = 240.dp),
        )
    }
}
