package ai.rojan.designlab.ui.components.state

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview

import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.material3.Text

/**
 * ROJAN AI shared empty-state primitive — State Experience Audit's
 * highest-priority gap: several customer-journey list screens
 * (Favorites, My Reviews, Appointments, Coupons, Search results)
 * currently render nothing at all when their list is empty. This is the
 * single component every one of those screens migrates onto, rather
 * than each hand-rolling its own inline message as `SalonListScreen`/
 * `WaitlistScreen` did.
 *
 * Deliberately wraps itself in [GlassSurface] rather than sitting bare
 * on the dark canvas — this makes it a self-contained "card" any screen
 * can drop into a `LazyColumn`/`Column` slot with zero surrounding
 * setup. Because content lives *inside* a light glass surface here (not
 * directly on [ai.rojan.designlab.ui.background.PremiumBackground]),
 * text uses the [RojanTextPrimary]/[RojanTextSecondary] family per the
 * codebase's existing "dark text ONLY on light cards" rule — not the
 * `RojanLuxury*` family, which is reserved for text sitting directly on
 * the dark canvas.
 *
 * [icon] defaults to a generic inbox glyph so every call site isn't
 * forced to pick one, but a screen-specific icon is expected in
 * practice (e.g. a heart outline for Favorites, a star for Reviews).
 * [actionLabel]/[onAction] are both-or-neither: the action row only
 * renders when both are supplied, so a purely informational empty state
 * (no obvious next action) doesn't get an orphaned button.
 *
 * RTL: every element is center-aligned in a vertical [Column], which
 * reads identically in LTR and RTL — no direction-specific mirroring
 * needed here, unlike a horizontally-arranged component would.
 */
@Composable
fun RojanEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector = Icons.Filled.Inbox,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RojanShapes.Small,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceXL),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RojanIconContainer(
                imageVector = icon,
                contentDescription = null,
                size = RojanIconSize.XLarge,
                tint = RojanTextSecondary,
            )

            Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))

            Text(
                text = title,
                style = RojanTypography.CardTitle,
                color = RojanTextPrimary,
                textAlign = TextAlign.Center,
            )

            if (description != null) {
                Spacer(modifier = Modifier.height(RojanDimens.SpaceXS))

                Text(
                    text = description,
                    style = RojanTypography.Body,
                    color = RojanTextSecondary,
                    textAlign = TextAlign.Center,
                )
            }

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

                PremiumButton(
                    text = actionLabel,
                    onClick = onAction,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF18233A)
@Composable
private fun RojanEmptyStatePreview() {
    RojanEmptyState(
        title = "چیزی یافت نشد",
        description = "هنوز موردی برای نمایش وجود ندارد",
        actionLabel = "مشاهده سالن‌ها",
        onAction = {},
    )
}
