package ai.rojan.designlab.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.presentation.booking.AppointmentDetailsData
import ai.rojan.designlab.presentation.booking.AppointmentDetailsViewModel
import ai.rojan.designlab.presentation.booking.AppointmentDetailsViewModelFactory
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.screens.customer.components.CustomerAccent
import ai.rojan.designlab.screens.customer.components.CustomerEmptyState
import ai.rojan.designlab.screens.customer.components.CustomerErrorState
import ai.rojan.designlab.screens.customer.components.CustomerLoadingState
import ai.rojan.designlab.screens.customer.components.CustomerScaffold
import ai.rojan.designlab.screens.customer.components.CustomerScreenMargin
import ai.rojan.designlab.screens.customer.components.CustomerSectionLabel
import ai.rojan.designlab.screens.customer.components.RefPrimaryButton
import ai.rojan.designlab.screens.customer.components.RefRowDivider
import ai.rojan.designlab.screens.customer.components.RefSurface
import ai.rojan.designlab.screens.customer.components.StatusPill
import ai.rojan.designlab.screens.customer.components.label
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Journey 2: Appointment details.
 *
 * Quiet Luxury pass (visual only). The `GlassBackButton` orb + ad-hoc
 * `HomeBackgroundTheme` shell + 32sp floating `HeroTitle`, the two glass
 * `HomeGlassSurface` cards (✦ sparkle corners, metallic border, glow), the
 * violet `Icons.Filled.Receipt`, the gradient `PremiumButton`, the glass
 * `RojanLoadingState` / `RojanErrorState` state views, the raw
 * `Text("نوبت یافت نشد")` empty, the raw-enum status ("PENDING") and
 * seconds-bearing date, and the trailing `RojanComingSoonState()` placeholder
 * are replaced with the [CustomerScaffold] shell and flat foundation
 * primitives: a flat [RefSurface] header, a divided invoice [RefSurface]
 * under a [CustomerSectionLabel], the shared [StatusPill], the
 * [CustomerLoadingState] / [CustomerErrorState] / [CustomerEmptyState]
 * states, and a pinned solid rose-gold [RefPrimaryButton] for rebook.
 *
 * NOTHING about behaviour changed: still `AppointmentDetailsViewModel` over
 * `GET /api/v1/bookings/{id}`; `viewModel.state` / `viewModel.retry()` read
 * in the same places; the rebook button still appears only when
 * `data.serviceName != null` and still calls
 * `onRebookClick(booking.serviceId, booking.salonId)` — same args, same
 * order. No ViewModel, repository, API, model, navigation route, or callback
 * is touched.
 *
 * (Unchanged from the previous pass: the reviews and photos sections stay
 * removed — no backend counterpart. `onRebookClick` still passes the real
 * `salonId` alongside `serviceId`.)
 */
@Composable
fun AppointmentDetailsScreen(
    appointmentId: String,
    onBackClick: () -> Unit,
    onRebookClick: (serviceId: String, salonId: String) -> Unit,
    viewModel: AppointmentDetailsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = run {
            val container = BackendApiContainerHolder.get(LocalContext.current)
            AppointmentDetailsViewModelFactory(
                appointmentId = appointmentId,
                bookingRepository = container.bookingRepository,
                salonRepository = container.salonRepository,
                specialistRepository = container.specialistRepository,
                serviceCategoryRepository = container.serviceCategoryRepository,
                serviceRepository = container.serviceRepository,
            )
        },
    ),
) {
    val data = (viewModel.state as? UiState.Success)?.data

    CustomerScaffold(
        title = "جزئیات نوبت",
        onBackClick = onBackClick,
        bottomBar = if (data?.serviceName != null) {
            {
                RefPrimaryButton(
                    label = "رزرو مجدد",
                    onClick = { onRebookClick(data.booking.serviceId, data.booking.salonId) },
                )
            }
        } else {
            null
        },
    ) {
        when (val state = viewModel.state) {
            is UiState.Loading -> CustomerLoadingState(
                modifier = Modifier.padding(top = RojanDimens.SpaceLG),
                count = 3,
                rowHeight = 92,
            )

            is UiState.Error -> CustomerErrorState(
                message = state.message,
                onRetry = { viewModel.retry() },
            )

            is UiState.Empty -> CustomerEmptyState(
                title = "نوبت یافت نشد",
                body = "این نوبت در دسترس نیست.",
                icon = Icons.Outlined.SearchOff,
            )

            is UiState.Success -> AppointmentDetailsContent(state.data)
        }
    }
}

@Composable
private fun AppointmentDetailsContent(data: AppointmentDetailsData) {
    val booking = data.booking

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = RojanDimens.SpaceLG),
    ) {
        RefSurface(modifier = Modifier.padding(horizontal = CustomerScreenMargin)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RojanDimens.SpaceMD),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    data.salonName ?: booking.salonId,
                    style = RojanTypography.Display.copy(fontSize = 26.sp, lineHeight = 34.sp),
                    color = HomeColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = listOfNotNull(data.serviceName, data.specialistName).joinToString(" • ")
                if (subtitle.isNotEmpty()) {
                    Spacer(Modifier.height(RojanDimens.SpaceXS))
                    Text(subtitle, style = RojanTypography.Body, color = HomeColors.TextSecondary)
                }
                Spacer(Modifier.height(RojanDimens.SpaceSM))
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
                Spacer(Modifier.height(RojanDimens.SpaceMD))
                StatusPill(booking.status)
            }
        }

        Spacer(Modifier.height(RojanDimens.SpaceXL))

        CustomerSectionLabel("رسید و فاکتور")
        Spacer(Modifier.height(RojanDimens.SpaceSM))
        RefSurface(modifier = Modifier.padding(horizontal = CustomerScreenMargin)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                data.serviceName?.let {
                    DetailRow("خدمت", it)
                    RefRowDivider()
                }
                data.servicePrice?.let {
                    DetailRow("مبلغ", "${it.toInt()} تومان", valueColor = CustomerAccent)
                    RefRowDivider()
                }
                DetailRow("وضعیت", booking.status.label())
                RefRowDivider()
                DetailRow("شماره پیگیری", booking.id, valueColor = HomeColors.TextMuted)
            }
        }

        Spacer(Modifier.height(RojanDimens.SpaceLG))
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = HomeColors.TextPrimary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RojanDimens.MinTouchTarget)
            .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceSM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            value,
            style = RojanTypography.Body,
            color = valueColor,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(RojanDimens.SpaceMD))
        Text(label, style = RojanTypography.Caption, color = HomeColors.TextMuted)
    }
}
