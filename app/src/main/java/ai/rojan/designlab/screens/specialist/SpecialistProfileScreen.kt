package ai.rojan.designlab.screens.specialist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.specialist.SpecialistProfileViewModel
import ai.rojan.designlab.presentation.specialist.SpecialistProfileViewModelFactory
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.image.MediaPreviewDialog
import ai.rojan.designlab.ui.components.image.RojanRemoteImage
import ai.rojan.designlab.ui.components.image.SpecialistAvatar
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.rtl.RtlListRow
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.components.state.RojanErrorState
import ai.rojan.designlab.ui.components.state.RojanLoadingState
import ai.rojan.designlab.ui.theme.RojanAquaMint
import ai.rojan.designlab.ui.theme.RojanBlushPink
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanPearlPink
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanSoftLavender
import ai.rojan.designlab.ui.theme.RojanTypography

/** Same deterministic-tint reasoning as [ai.rojan.designlab.screens.salon.SalonDetailsScreen]'s `accentFor`. */
private val accentPalette = listOf(RojanSoftLavender, RojanAquaMint, RojanBlushPink, RojanPearlPink)
private fun accentFor(id: String) = accentPalette[Math.floorMod(id.hashCode(), accentPalette.size)]

/**
 * Journey 1, Screen 3: Specialist Profile.
 *
 * **Android <-> Backend Full Integration milestone:** now backed by
 * [SpecialistProfileViewModel] -> `GET /api/v1/salons/{salonId}/specialists/{specialistId}`
 * plus the same category/service fan-out
 * [ai.rojan.designlab.screens.salon.SalonDetailsScreen] uses. [salonId] is
 * nullable — see `RojanDestinations.SPECIALIST_PROFILE`'s doc comment; when
 * absent, this shows a disclosed error rather than guessing.
 *
 * The backend `Specialist` has no experience-years/rating/completed-
 * appointments, skills list, or reviews concept — those sections are
 * removed rather than faked, same treatment as
 * [ai.rojan.designlab.screens.salon.SalonDetailsScreen]. `photoUrl` now
 * renders as a real remote image via [SpecialistAvatar]'s `photoUrl`
 * parameter (Coil, same seam [ai.rojan.designlab.screens.salon.SalonDetailsScreen]'s
 * specialists row already uses) — this screen previously called
 * [SpecialistAvatar] without it, always showing the icon fallback even
 * when a real photo existed; that was the actual gap, not a real backend
 * limitation.
 *
 * Media System Evolution v2: a "نمونه‌کارها" (portfolio) section renders
 * [SpecialistProfileData.portfolio] when non-empty, with the same
 * tap-to-preview [MediaPreviewDialog] the salon gallery and service images
 * sections use.
 */
@Composable
fun SpecialistProfileScreen(
    specialistId: String,
    salonId: String?,
    onBackClick: () -> Unit,
    onServiceClick: (String) -> Unit,
    viewModel: SpecialistProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = run {
            val container = BackendApiContainerHolder.get(LocalContext.current)
            SpecialistProfileViewModelFactory(
                salonId = salonId,
                specialistId = specialistId,
                specialistRepository = container.specialistRepository,
                serviceCategoryRepository = container.serviceCategoryRepository,
                serviceRepository = container.serviceRepository,
            )
        },
    ),
) {
    HomeBackgroundTheme {
        when (val loadState = viewModel.state) {
            is UiState.Loading -> SpecialistProfileScaffoldState(onBackClick) {
                RojanLoadingState(message = "در حال بارگذاری متخصص...")
            }
            is UiState.Empty -> SpecialistProfileScaffoldState(onBackClick) {
                RojanErrorState(title = "متخصص یافت نشد")
            }
            is UiState.Error -> SpecialistProfileScaffoldState(onBackClick) {
                RojanErrorState(description = loadState.message, actionLabel = "تلاش مجدد", onAction = viewModel::retry)
            }
            is UiState.Success -> {
                val specialist = loadState.data.specialist
                val services = loadState.data.services
                val portfolio = loadState.data.portfolio
                var previewIndex by remember { mutableStateOf<Int?>(null) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(RojanDimens.SpaceMD),
                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
                ) {
                    item { GlassBackButton(onClick = onBackClick) }

                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .background(accentFor(specialist.id).copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                SpecialistAvatar(
                                    assetRes = null,
                                    contentDescription = specialist.displayName,
                                    fallbackIconSize = 48.dp,
                                    modifier = Modifier.fillMaxSize(),
                                    photoUrl = specialist.photoUrl,
                                )
                            }
                            Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))
                            Text(specialist.displayName, style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary)
                        }
                    }

                    if (specialist.bio != null) {
                        item {
                            HomeGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
                                Text(
                                    text = specialist.bio,
                                    style = RojanTypography.Body,
                                    color = HomeColors.TextPrimary,
                                    modifier = Modifier.padding(RojanDimens.SpaceMD),
                                )
                            }
                        }
                    }

                    if (portfolio.isNotEmpty()) {
                        item { RtlSectionHeader("نمونه‌کارها", horizontalPadding = 0.dp, color = HomeColors.TextPrimary) }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                                itemsIndexed(portfolio) { index, imageUrl ->
                                    Box(
                                        modifier = Modifier
                                            .size(120.dp)
                                            .background(HomeColors.TextSecondary.copy(alpha = 0.15f), RojanShapes.Small)
                                            .rojanPressable(onClick = { previewIndex = index }),
                                    ) {
                                        RojanRemoteImage(
                                            url = imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxWidth().height(120.dp),
                                            shape = RojanShapes.Small,
                                            fallback = {},
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (services.isNotEmpty()) {
                        item { RtlSectionHeader("خدمات قابل رزرو", horizontalPadding = 0.dp, color = HomeColors.TextPrimary) }
                        itemsIndexed(services) { index, service ->
                            HomeGlassSurface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .rojanEnterAnimation(delayMillis = index * 60)
                                    .rojanPressable(onClick = { onServiceClick(service.id) }),
                                shape = RojanShapes.Small,
                            ) {
                                RtlListRow(
                                    title = service.name,
                                    titleColor = HomeColors.TextPrimary,
                                    value = "${service.durationMinutes} دقیقه",
                                    valueStyle = RojanTypography.Caption,
                                    valueColor = HomeColors.TextSecondary,
                                    modifier = Modifier.padding(RojanDimens.SpaceMD),
                                )
                            }
                        }
                    }
                }

                previewIndex?.let { index ->
                    MediaPreviewDialog(urls = portfolio, initialIndex = index, onDismiss = { previewIndex = null })
                }
            }
        }
    }
}

/** Shared loading/error/not-found scaffold: a back button above a centered state card. */
@Composable
private fun SpecialistProfileScaffoldState(onBackClick: () -> Unit, content: @Composable () -> Unit) {
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
