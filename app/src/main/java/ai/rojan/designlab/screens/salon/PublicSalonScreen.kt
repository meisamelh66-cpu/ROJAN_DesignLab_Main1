package ai.rojan.designlab.screens.salon

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.salon.PublicSalonData
import ai.rojan.designlab.presentation.salon.PublicSalonViewModel
import ai.rojan.designlab.presentation.salon.PublicSalonViewModelFactory
import ai.rojan.designlab.screens.customer.components.CustomerAccent
import ai.rojan.designlab.screens.customer.components.CustomerHairline
import ai.rojan.designlab.screens.customer.components.CustomerLoadingState
import ai.rojan.designlab.screens.customer.components.CustomerEmptyState
import ai.rojan.designlab.screens.customer.components.CustomerErrorState
import ai.rojan.designlab.screens.customer.components.CustomerScaffold
import ai.rojan.designlab.screens.customer.components.CustomerScreenMargin
import ai.rojan.designlab.screens.customer.components.CustomerSectionLabel
import ai.rojan.designlab.screens.customer.components.CustomerSurfaceFill
import ai.rojan.designlab.screens.customer.components.RefListRow
import ai.rojan.designlab.screens.customer.components.RefPrimaryButton
import ai.rojan.designlab.screens.customer.components.RefRowDivider
import ai.rojan.designlab.screens.customer.components.RefSurface
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.components.image.RojanRemoteImage
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * ROJAN AI Customer Journey — QR-scan / deep-link salon preview.
 *
 * Read-only public profile reached via a salon's public `slug`
 * ([ai.rojan.designlab.domain.repository.PublicSalonRepository], works
 * before any login). No Follow/Favorite, no service/specialist deep-dive,
 * no booking action — the one action is the login CTA.
 *
 * Quiet Luxury pass (visual only). The `GlassBackButton` orb, the bare
 * `HeroTitle`, the `HomeGlassSurface` info/specialist/service cards, the
 * violet `HomeColors.Glow` price, the `RtlInfoRow` / `RtlListRow` /
 * `RtlSectionHeader` primitives, the `salonAccentColorFor` logo tint, the
 * `RojanIconContainer` fallback, the glass `RojanLoadingState` /
 * `RojanErrorState`, and the gradient `PremiumButton` are replaced with the
 * [CustomerScaffold] shell (pinned rose-gold CTA in `bottomBar`) and the
 * flat foundation primitives: [RefSurface] cards, [RefListRow] rows,
 * [CustomerSectionLabel], outlined icons, [RefPrimaryButton], and
 * [CustomerLoadingState] / [CustomerEmptyState] / [CustomerErrorState].
 *
 * NOTHING about behaviour changed: still [PublicSalonViewModel] keyed on
 * `slug`; `retry()` and `onLoginClick` / `onBackClick` are called exactly
 * where they were. No ViewModel, repository, API, or navigation route is
 * touched.
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
    val state = viewModel.state

    CustomerScaffold(
        title = "سالن",
        onBackClick = onBackClick,
        bottomBar = if (state is UiState.Success) {
            { RefPrimaryButton(label = "ورود و رزرو نوبت", onClick = onLoginClick) }
        } else {
            null
        },
    ) {
        when (state) {
            is UiState.Loading -> CustomerLoadingState(
                modifier = Modifier.padding(top = RojanDimens.SpaceLG),
            )

            is UiState.Empty -> CustomerEmptyState(
                title = "سالن یافت نشد",
                icon = Icons.Outlined.SearchOff,
                actionLabel = "بازگشت",
                onAction = onBackClick,
            )

            is UiState.Error -> CustomerErrorState(
                message = state.message,
                onRetry = viewModel::retry,
            )

            is UiState.Success -> PublicSalonContent(data = state.data)
        }
    }
}

@Composable
private fun PublicSalonContent(data: PublicSalonData) {
    val salon = data.salon

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = RojanDimens.SpaceLG, bottom = RojanDimens.SpaceXL),
        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CustomerScreenMargin),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(CustomerSurfaceFill)
                        .border(1.dp, CustomerHairline, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    RojanRemoteImage(
                        url = salon.logoUrl,
                        contentDescription = salon.name,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxSize(),
                        fallback = {
                            Icon(
                                Icons.Outlined.Storefront,
                                contentDescription = null,
                                tint = HomeColors.TextMuted,
                                modifier = Modifier.size(28.dp),
                            )
                        },
                    )
                }
                Spacer(Modifier.height(RojanDimens.SpaceMD))
                Text(
                    salon.name,
                    style = RojanTypography.Display.copy(fontSize = 26.sp, lineHeight = 34.sp),
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
            }
        }

        item { Spacer(Modifier.height(RojanDimens.SpaceMD)) }

        item {
            RefSurface(modifier = Modifier.padding(horizontal = CustomerScreenMargin)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ContactRow(Icons.Outlined.Place, salon.address)
                    RefRowDivider()
                    ContactRow(Icons.Outlined.Phone, salon.phone)
                }
            }
        }

        if (data.specialists.isNotEmpty()) {
            item {
                Spacer(Modifier.height(RojanDimens.SpaceXL))
                CustomerSectionLabel("متخصصان")
                Spacer(Modifier.height(RojanDimens.SpaceSM))
            }
            item {
                RefSurface(modifier = Modifier.padding(horizontal = CustomerScreenMargin)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        data.specialists.forEachIndexed { index, specialist ->
                            if (index > 0) RefRowDivider()
                            RefListRow(
                                title = specialist.displayName,
                                subtitle = specialist.bio?.takeIf { it.isNotBlank() },
                                showChevron = false,
                            )
                        }
                    }
                }
            }
        }

        data.serviceGroups.filter { it.services.isNotEmpty() }.forEach { group ->
            item {
                Spacer(Modifier.height(RojanDimens.SpaceXL))
                CustomerSectionLabel(group.category.name)
                Spacer(Modifier.height(RojanDimens.SpaceSM))
            }
            item {
                RefSurface(modifier = Modifier.padding(horizontal = CustomerScreenMargin)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        group.services.forEachIndexed { index, service ->
                            if (index > 0) RefRowDivider()
                            RefListRow(
                                title = service.name,
                                subtitle = "${service.durationMinutes} دقیقه",
                                trailingValue = "${service.price.toInt()} تومان",
                                trailingValueColor = CustomerAccent,
                                showChevron = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceMD),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = HomeColors.TextMuted,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(
            value,
            style = RojanTypography.Caption,
            color = HomeColors.TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
