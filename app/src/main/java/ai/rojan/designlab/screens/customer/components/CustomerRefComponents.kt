package ai.rojan.designlab.screens.customer.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanPremiumBorderRoseGold
import ai.rojan.designlab.ui.theme.RojanTypography

/* =============================================================================
 * ROJAN Customer — DESIGN FOUNDATION (quiet luxury).
 *
 * The reusable primitives every Customer screen builds on, so the whole app
 * shares one flat "quiet luxury" surface language (Golden Reference:
 * docs/design-review/customer/REFERENCE-SPEC-salon-detail.md).
 *
 * This package is the single source of truth: the `bookingflow.components`
 * package now forwards here, and the un-migrated screens adopt these next. These are
 * screen-composition primitives — they add NO global design-system token and
 * edit NO shared component. Values are copied verbatim from the approved
 * screen-local `Ref*` set (SalonDetailsScreen.kt); promotion into `ui/theme`
 * is a later, separate step.
 *
 * Nothing here touches a ViewModel, repository, navigation route, or API.
 * ========================================================================== */

// --- Tokens ---------------------------------------------------------------

val CustomerScreenMargin = 20.dp
val CustomerCardRadius = 14.dp
val CustomerButtonRadius = 12.dp
val CustomerButtonHeight = 52.dp
val CustomerTopBarHeight = 56.dp

val CustomerAccent: Color = RojanPremiumBorderRoseGold          // #E0A67A — the single accent
val CustomerOnAccent: Color = Color(0xFF1B1530)                 // deep navy label on the accent
val CustomerSurfaceFill: Color = Color.White.copy(alpha = 0.045f)
val CustomerHairline: Color = Color.White.copy(alpha = 0.09f)
val CustomerDivider: Color = Color.White.copy(alpha = 0.07f)

val CustomerCardShape = RoundedCornerShape(CustomerCardRadius)
val CustomerSectionLabelStyle = RojanTypography.Caption.copy(fontWeight = FontWeight.SemiBold)

// --- Section label ----------------------------------------------------------

@Composable
fun CustomerSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = CustomerSectionLabelStyle,
        color = HomeColors.TextMuted,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CustomerScreenMargin),
    )
}

// --- Flat surface (glass only as a whisper of translucent lift) -------------

@Composable
fun RefSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CustomerCardShape)
            .background(CustomerSurfaceFill)
            .border(1.dp, CustomerHairline, CustomerCardShape),
    ) {
        content()
    }
}

@Composable
fun RefRowDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = RojanDimens.SpaceMD)
            .height(1.dp)
            .background(CustomerDivider),
    )
}

// --- List row (flush inside one RefSurface; RTL-anchored) -------------------

/**
 * One row in a divided list. RTL reading order: an optional [leading] visual
 * sits on the right, the title/subtitle column is right-anchored, an optional
 * [trailingValue] and the "leads forward" chevron sit on the left.
 */
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
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.rojanPressable(onClick = onClick, role = Role.Button)
                else Modifier,
            )
            .heightIn(min = RojanDimens.MinTouchTarget)
            .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceMD),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showChevron) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = null,
                tint = HomeColors.TextMuted,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(RojanDimens.SpaceSM))
        }
        trailingValue?.let {
            Text(it, style = RojanTypography.Caption, color = trailingValueColor)
        }
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                title,
                style = RojanTypography.Body,
                color = HomeColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(
                    it,
                    style = RojanTypography.Caption,
                    color = HomeColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        leading?.let {
            Spacer(Modifier.width(RojanDimens.SpaceMD))
            it()
        }
    }
}

// --- Selectable cell (dates / times / payment) -----------------------------

/**
 * A tappable cell that flips between a flat unselected surface and a solid
 * rose-gold selected fill. [content] receives the resolved content colour so
 * text/icons stay legible in both states.
 */
@Composable
fun RefSelectableCell(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: Boolean = true,
    content: @Composable RowScope.(contentColor: Color) -> Unit,
) {
    val contentColor = if (selected) CustomerOnAccent else HomeColors.TextPrimary
    Row(
        modifier = modifier
            .clip(CustomerCardShape)
            .background(if (selected) CustomerAccent else CustomerSurfaceFill)
            .border(1.dp, if (selected) CustomerAccent else CustomerHairline, CustomerCardShape)
            .rojanPressable(onClick = onClick, role = Role.Button)
            .heightIn(min = RojanDimens.MinTouchTarget)
            .then(
                if (contentPadding) {
                    Modifier.padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceSM)
                } else {
                    Modifier
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        content(contentColor)
    }
}

// --- Primary CTA (solid rose gold, no gradient) ----------------------------

@Composable
fun RefPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    // Root-cause fix (Customer Regression Pass): a disabled button gave no
    // visual difference between "processing" (e.g. OTP request in flight)
    // and "just disabled" — a real, bounded network wait (see
    // BackendApiContainer's NETWORK_TIMEOUT) read as a frozen/hung button.
    // Optional and defaulted `false` so every existing call site (booking
    // CTAs, dialogs, etc.) is unaffected unless it opts in.
    loading: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CustomerButtonHeight)
            .clip(RoundedCornerShape(CustomerButtonRadius))
            .background(CustomerAccent.copy(alpha = if (enabled) 1f else 0.4f))
            .then(
                if (enabled && !loading) Modifier.rojanPressable(onClick = onClick, role = Role.Button)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = CustomerOnAccent,
                strokeWidth = 2.5.dp,
            )
        } else {
            Text(
                label,
                style = RojanTypography.Button,
                color = CustomerOnAccent.copy(alpha = if (enabled) 1f else 0.6f),
            )
        }
    }
}

// --- Flat pulsing skeleton rows (shared by CustomerLoadingState) ----------

@Composable
internal fun CustomerSkeletonRows(
    modifier: Modifier = Modifier,
    count: Int = 4,
    rowHeight: Int = 64,
) {
    val transition = rememberInfiniteTransition(label = "customer-skeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "pulse",
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CustomerScreenMargin),
        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
    ) {
        repeat(count) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(rowHeight.dp)
                    .clip(CustomerCardShape)
                    .background(CustomerSurfaceFill)
                    .border(1.dp, CustomerHairline, CustomerCardShape)
                    .alpha(pulse),
            )
        }
    }
}
