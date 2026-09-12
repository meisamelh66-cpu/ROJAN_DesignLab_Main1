package ai.rojan.designlab.screens.customer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

/* =============================================================================
 * ROJAN Customer — loading / empty / error states.
 *
 * The quiet-luxury replacements for the shared glass `RojanLoadingState` /
 * `RojanEmptyState` / `RojanErrorState` cards. Flat, high-contrast, no card,
 * no glow, no gradient. Outlined icon, `RefPrimaryButton` for any action.
 *
 * A screen reader announces each state's content as soon as it appears
 * (liveRegion = Polite) — the moment a list finishes loading empty, or an
 * error replaces a loading state — with no call-site work.
 *
 * Touches no ViewModel, repository, navigation route, or API.
 * ========================================================================== */

/** Flat pulsing placeholder rows. Default: 4 rows, 64dp tall, on the margin. */
@Composable
fun CustomerLoadingState(
    modifier: Modifier = Modifier,
    count: Int = 4,
    rowHeight: Int = 64,
) {
    CustomerSkeletonRows(modifier = modifier, count = count, rowHeight = rowHeight)
}

/**
 * Calm centred empty state. Supply [actionLabel] + [onAction] together for a
 * primary CTA; omit both for a purely informational message.
 */
@Composable
fun CustomerEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    icon: ImageVector = Icons.Outlined.Inbox,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    CustomerCenteredState(
        icon = icon,
        title = title,
        body = body,
        actionLabel = actionLabel,
        onAction = onAction,
        modifier = modifier,
    )
}

/**
 * Calm centred error state. [onRetry] (with [retryLabel]) renders a primary
 * CTA; omit it for a terminal error.
 */
@Composable
fun CustomerErrorState(
    message: String,
    modifier: Modifier = Modifier,
    title: String = "مشکلی پیش آمد",
    icon: ImageVector = Icons.Outlined.CloudOff,
    retryLabel: String = "تلاش مجدد",
    onRetry: (() -> Unit)? = null,
) {
    CustomerCenteredState(
        icon = icon,
        title = title,
        body = message,
        actionLabel = if (onRetry != null) retryLabel else null,
        onAction = onRetry,
        modifier = modifier,
    )
}

@Composable
internal fun CustomerCenteredState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = CustomerScreenMargin)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = HomeColors.TextMuted, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(RojanDimens.SpaceMD))
        Text(title, style = RojanTypography.CardTitle, color = HomeColors.TextPrimary, textAlign = TextAlign.Center)
        body?.let {
            Spacer(Modifier.height(RojanDimens.SpaceXS))
            Text(it, style = RojanTypography.Caption, color = HomeColors.TextSecondary, textAlign = TextAlign.Center)
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(RojanDimens.SpaceLG))
            RefPrimaryButton(actionLabel, onAction, modifier = Modifier.widthIn(max = 240.dp))
        }
    }
}
