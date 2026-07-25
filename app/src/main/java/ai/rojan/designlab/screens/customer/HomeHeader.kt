package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextOnDarkSurface

/**
 * Customer Home header — Design Board v1.0, Section 1 (Header layer).
 *
 * Sits directly on the dark canvas ([PremiumBackground]), not on a glass
 * card, so per the Background System's two-surface text model, its
 * content uses the on-dark text tokens ([RojanTextOnGlass] for the
 * wordmark, [RojanTextOnDarkSurface] for the icons) rather than the
 * original light-system text tokens a glass-card surface would use.
 *
 * Deliberately compact and low visual weight per the Design Board's own
 * rule — this is identity + quick access, never competing with the Hero
 * Booking Area below it.
 *
 * Home Screen Production Pass, Task 10: the wordmark now carries an
 * explicit `lineHeight` (previously implicit/font-default, unlike every
 * other text style in the app, all of which set one via
 * [ai.rojan.designlab.ui.theme.RojanTypography]) — same 18sp/Bold/color
 * as before, "deliberately compact" per the rule above, so the size
 * itself is intentionally left as-is rather than bumped to a named
 * [ai.rojan.designlab.ui.theme.RojanTypography] tier.
 *
 * Task 9: both icons now render through [RojanIconContainer] — the
 * shared icon primitive every other icon in this screen already uses —
 * instead of a raw `Icon` + `Modifier.size(24.dp)`, and their size moves
 * onto the [RojanIconSize.Medium] token (20dp, this screen's standard
 * utility-icon tier) rather than a bespoke 24dp.
 */
@Composable
fun HomeHeader(
    onProfileClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "ROJAN AI",
            color = RojanTextOnGlass,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = RojanDimens.SubtitleSize,
                lineHeight = 24.sp,
            ),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RojanIconContainer(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "اعلان‌ها",
                tint = RojanTextOnDarkSurface,
                size = RojanIconSize.Medium,
                modifier = Modifier.clickable(onClick = onNotificationsClick),
            )

            RojanIconContainer(
                imageVector = Icons.Filled.Person,
                contentDescription = "پروفایل",
                tint = RojanTextOnDarkSurface,
                size = RojanIconSize.Medium,
                modifier = Modifier.clickable(onClick = onProfileClick),
            )
        }
    }
}
