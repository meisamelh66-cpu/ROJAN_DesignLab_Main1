package ai.rojan.designlab.screens.bookingflow

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.booking.PaymentMethod
import ai.rojan.designlab.domain.booking.RollingBookingDates
import ai.rojan.designlab.presentation.booking.BookingConfirmationViewModel
import ai.rojan.designlab.presentation.booking.BookingConfirmationViewModelFactory
import ai.rojan.designlab.presentation.booking.BookingSummary
import ai.rojan.designlab.presentation.booking.BookingViewModel
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.feedback.RojanSuccessCheckmark
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.components.rtl.RtlListRow
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.components.state.RojanErrorState
import ai.rojan.designlab.ui.theme.RojanAquaMint
import ai.rojan.designlab.ui.theme.RojanBlushPink
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanPearlPink
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanSoftLavender
import ai.rojan.designlab.ui.theme.RojanTypography
import kotlin.math.roundToInt

/**
 * Journey 1, Screen 7: Confirmation — summarizes [BookingViewModel]'s
 * accumulated state before final confirmation. Reads from the
 * session-scoped ViewModel passed in by Navigation, not a global
 * singleton — see [BookingViewModel]'s own doc comment for why that
 * distinction matters.
 *
 * Booking Experience Refactor: payment method selection lives on this
 * same screen rather than a separate "Payment" screen — the spec's flow
 * lists "Date & Time → Payment → Booking Success" as a distinct step,
 * but "Do NOT increase the number of booking steps" takes priority;
 * merging payment into the existing confirmation screen delivers the
 * same functionality without adding a step.
 *
 * Customer Journey Audit Phase A (P0-2) fix: the payment method selector
 * used to be local, screen-only UI state — selecting either option had
 * no effect beyond this screen. It now reads/writes [BookingViewModel.state.paymentMethod]
 * directly (via [BookingViewModel.onPaymentMethodSelected]), the same
 * event/reducer path every other field on this screen already goes
 * through, so the choice is genuinely recorded.
 */
@Composable
fun BookingConfirmationScreen(
    bookingViewModel: BookingViewModel,
    onBackClick: () -> Unit,
    onConfirmClick: (backendBookingId: String, summary: BookingSummary) -> Unit,
    onEditSalon: () -> Unit = {},
    onEditSpecialist: () -> Unit = {},
    onEditService: () -> Unit = {},
    onEditDate: () -> Unit = {},
    onEditTime: () -> Unit = {},
    confirmationViewModel: BookingConfirmationViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = BookingConfirmationViewModelFactory(
            bookingRepository = BackendApiContainerHolder.get(LocalContext.current).bookingRepository,
            salonRepository = BackendApiContainerHolder.get(LocalContext.current).salonRepository,
            specialistRepository = BackendApiContainerHolder.get(LocalContext.current).specialistRepository,
            serviceCategoryRepository = BackendApiContainerHolder.get(LocalContext.current).serviceCategoryRepository,
            serviceRepository = BackendApiContainerHolder.get(LocalContext.current).serviceRepository,
        ),
    ),
) {
    LaunchedEffect(bookingViewModel.state.salonId, bookingViewModel.state.specialistId, bookingViewModel.state.serviceId) {
        confirmationViewModel.loadSummary(
            salonId = bookingViewModel.state.salonId,
            specialistId = bookingViewModel.state.specialistId,
            serviceId = bookingViewModel.state.serviceId,
        )
    }
    val summary = confirmationViewModel.summary
    val salon = summary.salon
    val specialist = summary.specialist
    val service = summary.service
    val dateLabel = bookingViewModel.state.selectedDateKey?.let { RollingBookingDates.labelFor(it) }
    val time = bookingViewModel.state.selectedTime
    val selectedPaymentMethod = bookingViewModel.state.paymentMethod

    // Shared by the primary button and the error state's retry action
    // below, so a failed attempt can be retried without duplicating this
    // call — see BookingConfirmationViewModel.confirmBooking's contract.
    val confirmBooking: () -> Unit = {
        val state = bookingViewModel.state
        confirmationViewModel.confirmBooking(
            salonId = state.salonId,
            serviceId = state.serviceId,
            specialistId = state.specialistId,
            dateKey = state.selectedDateKey,
            time = state.selectedTime,
            onResult = { backendBookingId -> onConfirmClick(backendBookingId, confirmationViewModel.summary) },
        )
    }

    // Final Premium Polish, Phase 2: a one-shot fade + slight-upward-motion
    // entrance for the summary card — this screen is a decision/review
    // moment (per-row stagger would read as noisy here, not premium), so
    // one calm entrance for the whole card is the deliberate choice.
    var summaryVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { summaryVisible = true }

    HomeBackgroundTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // Phase 3 internal audit fix (found during live device
                    // verification, not theoretical): this Column wasn't
                    // scrollable, so once the 48dp-minimum touch-target fix
                    // on SummaryRow added real height across 6 rows, the
                    // payment method list overflowed past the bottom of the
                    // weight(1f) box and was silently clipped — "پرداخت در
                    // محل" stopped rendering on-device even though it was
                    // still in the tree. Scrolling is the correct fix, not
                    // shrinking the touch targets back down: this also
                    // protects the same content from the identical failure
                    // mode under large system font-scale settings, a real,
                    // separate accessibility case this screen had no
                    // protection against before.
                    .verticalScroll(rememberScrollState())
                    .padding(RojanDimens.SpaceMD),
            ) {
                GlassBackButton(onClick = onBackClick)

                Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))

                RtlSectionHeader("تایید رزرو", style = RojanTypography.HeroTitle, horizontalPadding = 0.dp, color = HomeColors.TextPrimary)

                Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

                HomeGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .rojanEnterAnimation(visible = summaryVisible),
                    shape = RojanShapes.GlassCard,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RojanDimens.SpaceMD),
                        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                    ) {
                        if (salon != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                            ) {
                                // Real backend Salon has no color/image concept — same
                                // deterministic-tint fallback already established in
                                // SalonListScreen.kt's MinimalSalonCard for this exact reason.
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(colorSeedFor(salon.id).copy(alpha = 0.5f), RojanShapes.Small),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.Storefront, contentDescription = null, tint = HomeColors.TextPrimary)
                                }
                                Text(salon.name, style = RojanTypography.Body, color = HomeColors.TextPrimary)
                            }
                        }
                        SummaryRow("سالن", salon?.name ?: "—", onClick = onEditSalon)
                        SummaryRow("متخصص", specialist?.displayName ?: "انتخاب خودکار", onClick = onEditSpecialist)
                        SummaryRow("خدمت", service?.name ?: "—", onClick = onEditService)
                        SummaryRow("تاریخ", dateLabel ?: "—", onClick = onEditDate)
                        SummaryRow("ساعت", time ?: "—", onClick = onEditTime)
                        if (service != null) {
                            SummaryRow(
                                "مبلغ قابل پرداخت",
                                "${service.price.roundToInt()} تومان",
                                highlight = true,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))

                RtlSectionHeader("روش پرداخت", horizontalPadding = 0.dp, color = HomeColors.TextPrimary)

                Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))

                PaymentMethod.entries.forEach { method ->
                    PaymentMethodRow(
                        method = method,
                        isSelected = selectedPaymentMethod == method,
                        onSelect = { bookingViewModel.onPaymentMethodSelected(method) },
                    )
                    Spacer(modifier = Modifier.height(RojanDimens.SpaceXS))
                }

                // Booking Transaction Integrity (TEAM2-001): confirmBooking's
                // onResult now only fires on a genuinely confirmed, persisted
                // backend booking — a failed attempt lands here instead, with
                // a real retry path (same confirm() call again), rather than
                // silently proceeding to BookingSuccessScreen.
                val submitError = confirmationViewModel.submitError
                if (submitError != null) {
                    Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))
                    RojanErrorState(
                        description = submitError,
                        actionLabel = "تلاش مجدد",
                        onAction = { confirmBooking() },
                    )
                }
            }

            Box(modifier = Modifier.padding(RojanDimens.SpaceMD)) {
                PremiumButton(
                    text = "تایید نهایی رزرو",
                    onClick = { confirmBooking() },
                    enabled = bookingViewModel.isReadyForConfirmation() &&
                        !confirmationViewModel.isSubmitting &&
                        !confirmationViewModel.isLoadingSummary,
                )
            }
        }
    }
}

/** Deterministic per-salon tint — the backend has no per-salon color/branding concept. Same palette/approach as SalonListScreen.kt's MinimalSalonCard. */
private val salonCardPalette = listOf(RojanSoftLavender, RojanAquaMint, RojanBlushPink, RojanPearlPink)
private fun colorSeedFor(salonId: String) = salonCardPalette[Math.floorMod(salonId.hashCode(), salonCardPalette.size)]

/** Display label for [PaymentMethod] — kept in the UI layer, same pattern as every other domain enum in this app (e.g. [ai.rojan.designlab.domain.booking.BookingIntent] carries no labels either). */
private val PaymentMethod.label: String
    get() = when (this) {
        PaymentMethod.WALLET -> "کیف پول"
        PaymentMethod.PAY_AT_SALON -> "پرداخت در محل"
    }

@Composable
private fun PaymentMethodRow(method: PaymentMethod, isSelected: Boolean, onSelect: () -> Unit) {
    HomeGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            // Phase 3 internal audit (Accessibility): the row's real
            // selected/unselected state now lives in semantics at the
            // pressable container itself — Role.RadioButton + a
            // stateDescription — rather than only on a decorative child
            // icon's contentDescription (the icon below is now purely
            // visual, contentDescription = null on both its states).
            .semantics {
                role = Role.RadioButton
                stateDescription = if (isSelected) "انتخاب شده" else "انتخاب نشده"
            }
            .rojanPressable(onClick = onSelect),
        shape = RojanShapes.Small,
    ) {
        RtlListRow(
            title = method.label,
            titleColor = HomeColors.TextPrimary,
            modifier = Modifier.padding(RojanDimens.SpaceMD),
            // Final Premium Polish, Phase 2: the selected state plays
            // RojanSuccessCheckmark's animated beat (Phase 1) instead of a
            // static icon swap — reads as "this choice was just confirmed,"
            // not just a different glyph appearing.
            //
            // Phase 3 internal audit (Animation issues): the checkmark used
            // to be conditionally composed (`if (isSelected) Checkmark else
            // Icon`), so switching *away* from a selected method removed it
            // from composition instantly — animated in, but never animated
            // out, a real asymmetry. Both the outline icon and the
            // checkmark now stay mounted together; only their alpha/
            // [RojanSuccessCheckmark]'s own `visible` toggles, so the
            // outgoing state gets the same animated beat the incoming one
            // does.
            trailing = {
                Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = HomeColors.TextSecondary,
                        modifier = Modifier.alpha(if (isSelected) 0f else 1f),
                    )
                    RojanSuccessCheckmark(visible = isSelected, size = 24.dp)
                }
            },
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String, highlight: Boolean = false, onClick: () -> Unit = {}) {
    // RTL fix: label now anchors right (via RtlListRow's title slot) and
    // value(+chevron) sits to its left — same information, right-anchored
    // reading direction. The chevron itself is still the deliberately
    // non-AutoMirrored, always-left-pointing KeyboardArrowLeft (see its own
    // note below): it means "leads forward to an edit screen," a meaning
    // that doesn't flip with RTL, so it isn't touched by this row's
    // direction fix.
    RtlListRow(
        title = label,
        titleColor = HomeColors.TextSecondary,
        value = value,
        valueColor = if (highlight) HomeColors.Glow else HomeColors.TextPrimary,
        // Not shown on the price row (highlight=true, and it isn't
        // independently editable — it's derived from the service above it).
        trailingIcon = if (!highlight) Icons.Filled.KeyboardArrowLeft else null,
        // Phase 3 internal audit (Accessibility): rows previously sized to
        // their text's line height alone (~24dp) — well under the 48dp
        // minimum touch target the rest of the app already holds itself to.
        // RtlListRow applies RojanDimens.MinTouchTarget itself whenever
        // onClick is provided, closing that gap here too.
        onClick = onClick,
    )
}
