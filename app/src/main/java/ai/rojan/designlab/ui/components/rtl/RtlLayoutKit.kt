package ai.rojan.designlab.ui.components.rtl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanRatingGold
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * ROJAN RTL layout kit — reusable, right-anchored building blocks for the
 * handful of content-row/header shapes that repeat across screens
 * (rating/address/hours/phone rows, label-value summary rows, icon+title
 * list rows, section headers).
 *
 * Deliberately scoped narrower than "wrap everything in
 * [androidx.compose.ui.unit.LayoutDirection.Rtl]": that exact mechanism was
 * tried app-wide earlier in this project and rejected — it mirrored salon/
 * service/specialist card photo-vs-text composition and reversed back-button
 * placement, breaking the frozen Design Baseline and the app's one
 * consistent back-button position. These components achieve the same
 * visual result (icon-right/text-left, right-anchored content) through
 * explicit composition order and [Alignment.End] instead, so:
 * - Every screen's back button (via
 *   [ai.rojan.designlab.ui.components.navigation.GlassBackButton] or its
 *   inline equivalents) stays exactly where it already is — untouched by
 *   this kit, on purpose.
 * - Image/photo compositions (salon thumbnails, avatars) are untouched —
 *   only small icon+text/label+value rows are in scope here, since photo
 *   mirroring was the specific thing the earlier experiment got wrong.
 * - Per-string text alignment already comes from
 *   [ai.rojan.designlab.ui.text.Text] (right-aligned Persian, left-aligned
 *   Latin, decided per string) — this kit only decides *container*
 *   placement, the piece that per-string alignment alone can't fix.
 */

/**
 * A section/page title that actually right-anchors: bare `Text` composables
 * elsewhere in this app already resolve Persian strings to
 * `TextAlign.Right` (see [ai.rojan.designlab.ui.text.resolveTextDirectionality]),
 * but that has no visible effect unless the text's own box actually spans
 * the full row width — most section headers were only ever as wide as
 * their own content, so the correct alignment was set but invisible. This
 * adds the `fillMaxWidth()` that makes it visible, nothing else.
 */
@Composable
fun RtlSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = RojanTypography.Body,
    color: Color = RojanTextOnGlass,
    horizontalPadding: Dp = RojanDimens.SpaceMD,
) {
    Text(
        text,
        style = style,
        color = color,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
    )
}

/**
 * One icon+text information row, icon trailing (right) and text leading
 * (left of the icon) — the mirror image of this app's older icon-first
 * rows. Caller is responsible for right-anchoring the row itself within
 * its container (e.g. a parent `Column(horizontalAlignment = Alignment.End)`)
 * when several of these stack together, same as
 * [ai.rojan.designlab.screens.salon.SalonDetailsScreen]'s rating/address/
 * hours/phone block.
 */
@Composable
fun RtlInfoRow(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false,
    iconTint: Color = if (emphasize) RojanRatingGold else RojanTextSecondary,
    iconSize: Dp = 20.dp,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            style = if (emphasize) RojanTypography.Body else RojanTypography.Caption,
            color = if (emphasize) RojanTextPrimary else RojanTextSecondary,
        )
        Spacer(modifier = Modifier.width(RojanDimens.SpaceSM))
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(iconSize))
    }
}

/**
 * General-purpose right-anchored list row: an optional value/trailing
 * element on the left, and title (+ optional subtitle, + optional leading
 * icon rendered at the physical right edge) on the right — covers the
 * label-value summary row (Booking Confirmation), the icon+title+subtitle
 * menu row (Profile), and the title+duration row (Specialist Profile/Salon
 * Details service lists) with one component instead of a bespoke Row per
 * screen.
 *
 * The right-hand title block always takes the remaining row width via
 * `weight(1f)` (regardless of whether a value/trailing exists), so a short
 * title still sits flush against the icon/right edge — the correct RTL
 * reading pinned at the leading (right) side of its available space, not
 * artificially stretched or centered.
 */
@Composable
fun RtlListRow(
    title: String,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = RojanTypography.Body,
    titleColor: Color = RojanTextPrimary,
    subtitle: String? = null,
    subtitleStyle: TextStyle = RojanTypography.Caption,
    subtitleColor: Color = RojanTextSecondary,
    icon: ImageVector? = null,
    iconTint: Color = RojanAIGlow,
    value: String? = null,
    valueStyle: TextStyle = RojanTypography.Body,
    valueColor: Color = RojanTextPrimary,
    trailingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .heightIn(min = RojanDimens.MinTouchTarget)
                        .rojanPressable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (value != null || trailing != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
            ) {
                value?.let {
                    Text(it, style = valueStyle, color = valueColor)
                }
                when {
                    trailing != null -> trailing()
                    trailingIcon != null -> RojanIconContainer(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = RojanTextSecondary,
                        size = RojanIconSize.Small,
                    )
                }
            }
            Spacer(modifier = Modifier.width(RojanDimens.SpaceSM))
        }

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
            ) {
                Text(title, style = titleStyle, color = titleColor, modifier = Modifier.fillMaxWidth())
                subtitle?.let {
                    Text(it, style = subtitleStyle, color = subtitleColor, modifier = Modifier.fillMaxWidth())
                }
            }
            icon?.let {
                Spacer(modifier = Modifier.width(RojanDimens.SpaceSM))
                Icon(it, contentDescription = null, tint = iconTint)
            }
        }
    }
}
