package ai.rojan.designlab.screens.salon

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.salon.PublicSalonData
import ai.rojan.designlab.presentation.salon.PublicSalonViewModel
import ai.rojan.designlab.presentation.salon.PublicSalonViewModelFactory
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.image.RojanRemoteImage
import ai.rojan.designlab.ui.components.image.SpecialistAvatar
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.rtl.RtlInfoRow
import ai.rojan.designlab.ui.components.rtl.RtlListRow
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.components.state.RojanErrorState
import ai.rojan.designlab.ui.components.state.RojanLoadingState
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.theme.salonAccentColorFor

/**
 * ROJAN AI Customer Journey Phase 2 (P0-2): QR-scan/deep-link entry point.
 * Read-only preview of a salon reached via its public `slug` (`ROJAN_Backend`'s
 * `PublicSalonController`, unauthenticated by design) — same visual system
 * as [ai.rojan.designlab.screens.salon.SalonDetailsScreen] (frozen Design
 * Baseline v1.0 primitives), but deliberately not that screen reused as-is:
 * this one's data source is genuinely public
 * ([ai.rojan.designlab.domain.repository.PublicSalonRepository]), so it
 * works before any login exists, whereas [SalonDetailsScreen] is backed by
 * the authenticated [ai.rojan.designlab.domain.repository.SalonRepository].
 * No Follow/Favorite, no service/specialist deep-dive, no booking action —
 * association with a salon still only happens the same way it always has
 * (the customer's first real booking, via the backend's own
 * `EnsureCustomerAssociationUseCase`), not from a bare scan. The one action
 * here is the login CTA.
 */
@Composable
fun PublicSalonScreen(
    slug: String,
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    viewModel: PublicSalonViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        key = "public_salon_$slug",
        factory = run {
            val container = BackendApiContainerHolder.get(LocalContext.current)
            PublicSalonViewModelFactory(slug = slug, publicSalonRepository = container.publicSalonRepository)
        },
    ),
) {
    HomeBackgroundTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.padding(RojanDimens.SpaceMD)) {
                GlassBackButton(onClick = onBackClick)
            }

            when (val loadState = viewModel.state) {
                is UiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    RojanLoadingState(message = "در حال بارگذاری سالن...")
                }
                is UiState.Empty -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    RojanErrorState(title = "سالن یافت نشد", actionLabel = "بازگشت", onAction = onBackClick)
                }
                is UiState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    RojanErrorState(description = loadState.message, actionLabel = "تلاش مجدد", onAction = viewModel::retry)
                }
                is UiState.Success -> PublicSalonContent(data = loadState.data, onLoginClick = onLoginClick)
            }
        }
    }
}

@Composable
private fun PublicSalonContent(data: PublicSalonData, onLoginClick: () -> Unit) {
    val salon = data.salon
    val tint = salonAccentColorFor(salon.id)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = RojanDimens.SpaceMD),
        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .background(tint.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    RojanRemoteImage(
                        url = salon.logoUrl,
                        contentDescription = salon.name,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxSize(),
                        fallback = {
                            RojanIconContainer(
                                imageVector = Icons.Filled.Storefront,
                                contentDescription = null,
                                tint = HomeColors.TextPrimary,
                                size = RojanIconSize.Large,
                            )
                        },
                    )
                }
                Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))
                Text(salon.name, style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary)
                salon.description?.let {
                    Text(it, style = RojanTypography.Body, color = HomeColors.TextSecondary)
                }
            }
        }

        item {
            HomeGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small, glassAlpha = 0.28f, glassSecondaryAlpha = 0.10f) {
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

        if (data.specialists.isNotEmpty()) {
            item { RtlSectionHeader("متخصصان", color = HomeColors.TextPrimary) }
            itemsIndexed(data.specialists) { index, specialist ->
                HomeGlassSurface(modifier = Modifier.fillMaxWidth().rojanEnterAnimation(delayMillis = index * 60), shape = RojanShapes.Small) {
                    Box(modifier = Modifier.padding(RojanDimens.SpaceMD)) {
                        RtlListRow(
                            title = specialist.displayName,
                            titleColor = HomeColors.TextPrimary,
                            subtitle = specialist.bio,
                            subtitleColor = HomeColors.TextSecondary,
                        )
                    }
                }
            }
        }

        data.serviceGroups.filter { it.services.isNotEmpty() }.forEach { group ->
            item { RtlSectionHeader(group.category.name, color = HomeColors.TextPrimary) }
            itemsIndexed(group.services) { index, service ->
                HomeGlassSurface(modifier = Modifier.fillMaxWidth().rojanEnterAnimation(delayMillis = index * 60), shape = RojanShapes.Small) {
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

        item {
            PremiumButton(
                text = "ورود و رزرو نوبت",
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = RojanDimens.SpaceMD)
                    .height(RojanDimens.ButtonHeight),
            )
        }
    }
}
