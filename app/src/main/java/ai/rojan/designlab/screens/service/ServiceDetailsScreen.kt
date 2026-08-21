package ai.rojan.designlab.screens.service

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.Icon
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
import ai.rojan.designlab.presentation.service.ServiceDetailsViewModel
import ai.rojan.designlab.presentation.service.ServiceDetailsViewModelFactory
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.image.MediaPreviewDialog
import ai.rojan.designlab.ui.components.image.RojanRemoteImage
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
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

/** Same deterministic-tint reasoning as [ai.rojan.designlab.screens.salon.SalonDetailsScreen]'s `accentFor` — the backend `Service` has no color/branding concept. */
private val accentPalette = listOf(RojanSoftLavender, RojanAquaMint, RojanBlushPink, RojanPearlPink)
private fun accentFor(id: String) = accentPalette[Math.floorMod(id.hashCode(), accentPalette.size)]

/**
 * Journey 1, Screen 4: Service Details.
 *
 * **Android <-> Backend Full Integration milestone:** now backed by
 * [ServiceDetailsViewModel] -> the same category fan-out
 * [ai.rojan.designlab.screens.salon.SalonDetailsScreen] uses (no
 * salon-wide or id-only service lookup exists on the backend). [salonId]
 * comes from the shared booking flow state
 * (`BookingViewModel.state.salonId` at the `RojanNavGraph.kt` call site) —
 * on the one navigation path that doesn't set it (rebooking from a past
 * appointment, still demo/local-state territory), the screen shows a clear
 * error instead of guessing.
 *
 * The backend `Service` has no discount-price, benefits list,
 * preparation/aftercare copy, or color-branding concept — those sections
 * are removed rather than faked, same disclosed-gap treatment as
 * [ai.rojan.designlab.screens.salon.SalonDetailsScreen].
 */
@Composable
fun ServiceDetailsScreen(
    serviceId: String,
    salonId: String?,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    viewModel: ServiceDetailsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = run {
            val container = BackendApiContainerHolder.get(LocalContext.current)
            ServiceDetailsViewModelFactory(
                salonId = salonId,
                serviceId = serviceId,
                serviceCategoryRepository = container.serviceCategoryRepository,
                serviceRepository = container.serviceRepository,
            )
        },
    ),
) {
    HomeBackgroundTheme {
        when (val loadState = viewModel.state) {
            is UiState.Loading -> ServiceDetailsScaffoldState(onBackClick) {
                RojanLoadingState(message = "در حال بارگذاری خدمت...")
            }
            is UiState.Empty -> ServiceDetailsScaffoldState(onBackClick) {
                RojanErrorState(title = "خدمت یافت نشد")
            }
            is UiState.Error -> ServiceDetailsScaffoldState(onBackClick) {
                RojanErrorState(description = loadState.message, actionLabel = "تلاش مجدد", onAction = viewModel::retry)
            }
            is UiState.Success -> {
                val service = loadState.data.service
                val images = loadState.data.images
                var previewIndex by remember { mutableStateOf<Int?>(null) }

                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(RojanDimens.SpaceMD),
                        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
                    ) {
                        item { GlassBackButton(onClick = onBackClick) }

                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(accentFor(service.id).copy(alpha = 0.5f), RojanShapes.GlassCard),
                                contentAlignment = Alignment.Center,
                            ) {
                                RojanIconContainer(
                                    imageVector = Icons.Filled.ContentCut,
                                    contentDescription = null,
                                    tint = HomeColors.TextPrimary,
                                    sizeOverride = 48.dp,
                                )
                            }
                        }

                        item {
                            Column {
                                Text(service.name, style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.AccessTime, null, tint = HomeColors.TextSecondary, modifier = Modifier.size(RojanDimens.IconSizeSmall))
                                    Text(" ${service.durationMinutes} دقیقه", style = RojanTypography.Caption, color = HomeColors.TextSecondary)
                                }
                            }
                        }

                        item {
                            Text(
                                text = "${service.price.toInt()} تومان",
                                style = RojanTypography.HeroTitle,
                                color = HomeColors.Glow,
                            )
                        }

                        if (service.description != null) {
                            item {
                                HomeGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
                                    Text(
                                        text = service.description,
                                        style = RojanTypography.Body,
                                        color = HomeColors.TextPrimary,
                                        modifier = Modifier.padding(RojanDimens.SpaceMD),
                                    )
                                }
                            }
                        }

                        if (images.isNotEmpty()) {
                            item { RtlSectionHeader("تصاویر", horizontalPadding = 0.dp, color = HomeColors.TextPrimary) }
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                                    itemsIndexed(images) { index, imageUrl ->
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
                    }

                    Box(modifier = Modifier.padding(RojanDimens.SpaceMD)) {
                        PremiumButton(
                            text = "رزرو این خدمت",
                            onClick = { onBookClick(service.id) },
                        )
                    }
                }

                previewIndex?.let { index ->
                    MediaPreviewDialog(urls = images, initialIndex = index, onDismiss = { previewIndex = null })
                }
            }
        }
    }
}

/** Shared loading/error/not-found scaffold: a back button above a centered state card. */
@Composable
private fun ServiceDetailsScaffoldState(onBackClick: () -> Unit, content: @Composable () -> Unit) {
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
