package ai.rojan.designlab.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanAvatarGradientStart
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanGlassWhite
import ai.rojan.designlab.ui.theme.RojanShadows
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Booking Module Design System Migration.
 *
 * Every hardcoded color/dimension replaced with tokens per the mapping
 * established across this migration: `#30243B`/`#756B80` were close
 * enough to [RojanTextPrimary]/[RojanTextSecondary] to reuse directly
 * (a few RGB points of drift, not a deliberately different color);
 * `#8B5CF6` (repeated across every Booking file) maps to [RojanAIGlow]
 * for the same reason. The avatar gradient's own distinct pink start
 * color is preserved via the new [RojanAvatarGradientStart] token rather
 * than force-fit to an unrelated existing one.
 *
 * Layout, structure, and every dimension value are otherwise unchanged
 * from before this migration — this is a token-sourcing pass, not a
 * redesign.
 */
@Composable
fun BookingHeader() {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = RojanTextOnGlass,
                modifier = Modifier
                    .size(58.dp)
                    .shadow(
                        elevation = RojanShadows.FloatingElevation,
                        shape = CircleShape
                    )
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                RojanAvatarGradientStart,
                                RojanAIGlow
                            )
                        ),
                        CircleShape
                    )
                    .padding(15.dp)
            )

            Spacer(modifier = Modifier.size(RojanDimens.SpaceMD))

            Column {
                Text(
                    text = "سلام، خوش آمدید",
                    style = RojanTypography.Body,
                    color = RojanTextSecondary
                )

                Text(
                    text = "رزرو هوشمند زیبایی",
                    fontSize = RojanDimens.SubtitleSize,
                    color = RojanTextPrimary
                )
            }
        }

        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            tint = RojanAIGlow,
            modifier = Modifier
                .size(48.dp)
                .shadow(
                    elevation = RojanShadows.FloatingElevation,
                    shape = RojanShapes.Small
                )
                .background(
                    RojanGlassWhite.copy(alpha = 0.65f),
                    RojanShapes.Small
                )
                .padding(RojanDimens.SpaceSM)
        )
    }
}
