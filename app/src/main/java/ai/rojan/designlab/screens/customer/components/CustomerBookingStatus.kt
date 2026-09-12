package ai.rojan.designlab.screens.customer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanTypography

/* =============================================================================
 * ROJAN Customer — booking status display.
 *
 * One Persian label + one calm tint per [BookingStatus], plus the flat pill
 * that renders them. Shared so every place that shows a booking's status —
 * the appointments list card and the appointment detail — reads identically.
 *
 * Touches no ViewModel, repository, navigation route, or API.
 * ========================================================================== */

/** User-facing Persian label for a booking status. */
fun BookingStatus.label(): String = when (this) {
    BookingStatus.PENDING -> "در انتظار تایید"
    BookingStatus.CONFIRMED -> "تایید شده"
    BookingStatus.COMPLETED -> "انجام شده"
    BookingStatus.CANCELLED -> "لغو شده"
}

/**
 * The calm accent for a status: rose-gold for a live confirmed booking,
 * neutral muted/secondary for pending/done, the error rose-red for cancelled.
 */
val BookingStatus.tint: Color
    @Composable get() = when (this) {
        BookingStatus.CONFIRMED -> CustomerAccent
        BookingStatus.PENDING -> HomeColors.TextMuted
        BookingStatus.COMPLETED -> HomeColors.TextSecondary
        BookingStatus.CANCELLED -> RojanErrorText
    }

/** Flat tinted pill — the status label on a 14%-opacity wash of its own [tint]. */
@Composable
fun StatusPill(status: BookingStatus, modifier: Modifier = Modifier) {
    val color = status.tint
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = RojanDimens.SpaceSM, vertical = 3.dp),
    ) {
        Text(
            status.label(),
            style = RojanTypography.Caption,
            color = color,
            maxLines = 1,
        )
    }
}
