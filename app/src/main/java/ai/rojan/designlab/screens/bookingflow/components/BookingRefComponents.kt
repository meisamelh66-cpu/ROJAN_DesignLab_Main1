package ai.rojan.designlab.screens.bookingflow.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

import ai.rojan.designlab.screens.customer.components.CustomerAccent
import ai.rojan.designlab.screens.customer.components.CustomerButtonHeight
import ai.rojan.designlab.screens.customer.components.CustomerButtonRadius
import ai.rojan.designlab.screens.customer.components.CustomerCardRadius
import ai.rojan.designlab.screens.customer.components.CustomerCardShape
import ai.rojan.designlab.screens.customer.components.CustomerDivider
import ai.rojan.designlab.screens.customer.components.CustomerEmptyState
import ai.rojan.designlab.screens.customer.components.CustomerHairline
import ai.rojan.designlab.screens.customer.components.CustomerLoadingState
import ai.rojan.designlab.screens.customer.components.CustomerOnAccent
import ai.rojan.designlab.screens.customer.components.CustomerScreenMargin
import ai.rojan.designlab.screens.customer.components.CustomerSectionLabel
import ai.rojan.designlab.screens.customer.components.CustomerSectionLabelStyle
import ai.rojan.designlab.screens.customer.components.CustomerSurfaceFill
import ai.rojan.designlab.screens.customer.components.CustomerTopBarHeight
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.components.RefListRow as CustomerRefListRow
import ai.rojan.designlab.screens.customer.components.RefPrimaryButton as CustomerRefPrimaryButton
import ai.rojan.designlab.screens.customer.components.RefRowDivider as CustomerRefRowDivider
import ai.rojan.designlab.screens.customer.components.RefSelectableCell as CustomerRefSelectableCell
import ai.rojan.designlab.screens.customer.components.RefSurface as CustomerRefSurface

/* =============================================================================
 * ROJAN Customer — booking-flow foundation (compatibility layer).
 *
 * The real implementation now lives in
 * `ai.rojan.designlab.screens.customer.components.*` (the Customer design
 * foundation). This file forwards the `Booking*` names the already-migrated
 * booking screens import, so they keep compiling unchanged during the
 * app-wide migration. New code should import from `customer.components`
 * directly; these forwarders can be deleted once the booking screens do.
 * ========================================================================== */

// --- Tokens (aliases) ------------------------------------------------------

val BookingScreenMargin = CustomerScreenMargin
val BookingCardRadius = CustomerCardRadius
val BookingButtonRadius = CustomerButtonRadius
val BookingButtonHeight = CustomerButtonHeight
val BookingTopBarHeight = CustomerTopBarHeight

val BookingAccent: Color = CustomerAccent
val BookingOnAccent: Color = CustomerOnAccent
val BookingSurfaceFill: Color = CustomerSurfaceFill
val BookingHairline: Color = CustomerHairline
val BookingDivider: Color = CustomerDivider

val BookingCardShape = CustomerCardShape
val BookingSectionLabelStyle = CustomerSectionLabelStyle

// --- Composables (forwarders) -------------------------------------------

@Composable
fun BookingSectionLabel(text: String, modifier: Modifier = Modifier) =
    CustomerSectionLabel(text = text, modifier = modifier)

@Composable
fun RefSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    CustomerRefSurface(modifier = modifier, content = content)

@Composable
fun RefRowDivider(modifier: Modifier = Modifier) =
    CustomerRefRowDivider(modifier = modifier)

@Composable
fun RefListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingValue: String? = null,
    trailingValueColor: Color = HomeColors.TextMuted,
    leading: (@Composable () -> Unit)? = null,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null,
) = CustomerRefListRow(
    title = title,
    modifier = modifier,
    subtitle = subtitle,
    trailingValue = trailingValue,
    trailingValueColor = trailingValueColor,
    leading = leading,
    showChevron = showChevron,
    onClick = onClick,
)

@Composable
fun RefSelectableCell(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: Boolean = true,
    content: @Composable RowScope.(contentColor: Color) -> Unit,
) = CustomerRefSelectableCell(
    selected = selected,
    onClick = onClick,
    modifier = modifier,
    contentPadding = contentPadding,
    content = content,
)

@Composable
fun RefPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = CustomerRefPrimaryButton(label = label, onClick = onClick, modifier = modifier, enabled = enabled)

@Composable
fun BookingLoadingRows(
    modifier: Modifier = Modifier,
    count: Int = 4,
    rowHeight: Int = 64,
) = CustomerLoadingState(modifier = modifier, count = count, rowHeight = rowHeight)

@Composable
fun BookingCenteredState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) = CustomerEmptyState(
    title = title,
    modifier = modifier,
    body = body,
    icon = icon,
    actionLabel = actionLabel,
    onAction = onAction,
)
