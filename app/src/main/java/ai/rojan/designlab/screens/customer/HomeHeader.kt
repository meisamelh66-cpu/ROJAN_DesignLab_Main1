package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
            fontWeight = FontWeight.Bold,
            fontSize = RojanDimens.SubtitleSize,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "اعلان‌ها",
                tint = RojanTextOnDarkSurface,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onNotificationsClick)
            )

            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "پروفایل",
                tint = RojanTextOnDarkSurface,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onProfileClick)
            )
        }
    }
}
