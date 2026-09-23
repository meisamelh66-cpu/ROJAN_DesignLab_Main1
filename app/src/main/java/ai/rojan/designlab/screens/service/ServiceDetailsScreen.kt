package ai.rojan.designlab.screens.service

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.repository.Service
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.service.ServiceDetailsViewModel
import ai.rojan.designlab.presentation.service.ServiceDetailsViewModelFactory
import ai.rojan.designlab.screens.bookingflow.components.BookingAccent
import ai.rojan.designlab.screens.bookingflow.components.BookingCenteredState
import ai.rojan.designlab.screens.bookingflow.components.BookingDivider
import ai.rojan.designlab.screens.bookingflow.components.BookingLoadingRows
import ai.rojan.designlab.screens.bookingflow.components.BookingScaffold
import ai.rojan.designlab.screens.bookingflow.components.BookingScreenMargin
import ai.rojan.designlab.screens.bookingflow.components.BookingSectionLabelStyle
import ai.rojan.designlab.screens.bookingflow.components.RefPrimaryButton
import ai.rojan.designlab.screens.bookingflow.components.RefSurface
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Journey 1, Screen 4: Service Details.
 *
 * Quiet Luxury pass (visual only): the floating `GlassBackButton` orb, the
 * 140dp `accentFor()` colour hero band with the 48dp filled `ContentCut`
 * scissors, the two competing `HeroTitle`s (name in white, price in violet),
 * and the `HomeGlassSurface` description card + gradient `PremiumButton` are
 * replaced with the [BookingScaffold] shell (step 2 of 5) and flat
 * `bookingflow.components`: a clear service-name headline, a `RefSurface`
 * duration / price strip with a rose-gold price, an optional `RefSurface`
 * description block, and a solid pinned [RefPrimaryButton].
 *
 * NOTHING about the data flow changed: [ServiceDetailsViewModel] still runs
 * the same category fan-out for [salonId] / [serviceId], the factory is
 * byte-identical, [onBookClick] still fires `service.id`, and the backend
 * `Service` (name / duration / price / description — no discount, benefits,
 * or branding) is rendered as-is, never faked. No ViewModel, booking
 * state/context, navigation, API call, or data model is modified here.
 */
@Composable
fun ServiceDetailsScreen(
    serviceId: String,
    salonId: String?,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    // Guest Booking Flow fix: the salon's public slug, threaded from the
    // shared BookingViewModel (see RojanNavGraph.kt) — non-null only when
    // this booking session's salon was reached via a guest-visible list.
    slug: String? = null,
    viewModel: ServiceDetailsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = run {
            val container = BackendApiContainerHolder.get(LocalContext.current)
            ServiceDetailsViewModelFactory(
                salonId = salonId,
                serviceId = serviceId,
                serviceCategoryRepository = container.serviceCategoryRepository,
                serviceRepository = container.serviceRepository,
                publicSalonRepository = container.publicSalonRepository,
                slug = slug,
                hasSession = { container.tokenRepository.accessToken()?.isNotBlank() == true },
            )
        },
    ),
) {
    val state = viewModel.state
    val service = (state as? UiState.Success)?.data

    BookingScaffold(
        title = service?.name ?: "جزئیات خدمت",
        onBackClick = onBackClick,
        step = 2,
        bottomBar = if (service != null) {
            { RefPrimaryButton(label = "رزرو این خدمت", onClick = { onBookClick(service.id) }) }
        } else {
            null
        },
    ) {
        when (state) {
            is UiState.Loading -> BookingLoadingRows(
                modifier = Modifier.padding(top = RojanDimens.SpaceLG),
                count = 3,
                rowHeight = 72,
            )

            is UiState.Empty -> BookingCenteredState(
                icon = Icons.Outlined.SearchOff,
                title = "خدمت یافت نشد",
                body = "این خدمت در دسترس نیست.",
                actionLabel = "بازگشت",
                onAction = onBackClick,
            )

            is UiState.Error -> BookingCenteredState(
                icon = Icons.Outlined.CloudOff,
                title = "مشکلی پیش آمد",
                body = state.message,
                actionLabel = "تلاش مجدد",
                onAction = { viewModel.retry() },
            )

            is UiState.Success -> ServiceContent(state.data)
        }
    }
}

@Composable
private fun ServiceContent(service: Service) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = BookingScreenMargin, vertical = RojanDimens.SpaceLG),
    ) {
        Text(
            service.name,
            style = RojanTypography.Display.copy(fontSize = 26.sp, lineHeight = 34.sp),
            color = HomeColors.TextPrimary,
        )

        Spacer(Modifier.height(RojanDimens.SpaceMD))

        RefSurface {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceMD),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetaCell(
                    label = "مبلغ",
                    value = "${service.price.toInt()} تومان",
                    valueColor = BookingAccent,
                    align = Alignment.Start,
                )
                Box(
                    Modifier
                        .width(1.dp)
                        .height(34.dp)
                        .background(BookingDivider),
                )
                MetaCell(
                    label = "مدت زمان",
                    value = "${service.durationMinutes} دقیقه",
                    valueColor = HomeColors.TextPrimary,
                    align = Alignment.End,
                    trailingIcon = true,
                )
            }
        }

        service.description?.takeIf { it.isNotBlank() }?.let { description ->
            Spacer(Modifier.height(RojanDimens.SpaceXL))
            Text(
                "درباره این خدمت",
                style = BookingSectionLabelStyle,
                color = HomeColors.TextMuted,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(RojanDimens.SpaceSM))
            RefSurface {
                Text(
                    description,
                    style = RojanTypography.Body,
                    color = HomeColors.TextSecondary,
                    modifier = Modifier.padding(RojanDimens.SpaceMD),
                )
            }
        }
    }
}

@Composable
private fun MetaCell(
    label: String,
    value: String,
    valueColor: Color,
    align: Alignment.Horizontal,
    trailingIcon: Boolean = false,
) {
    Column(horizontalAlignment = align) {
        Text(label, style = RojanTypography.Caption, color = HomeColors.TextMuted)
        Spacer(Modifier.height(RojanDimens.SpaceXS))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = RojanTypography.CardTitle, color = valueColor)
            if (trailingIcon) {
                Spacer(Modifier.width(RojanDimens.SpaceXS))
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = HomeColors.TextMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
