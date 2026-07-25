package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.interaction.rojanPressedShadow
import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize

/**
 * Customer Home AI search bar — Design Board v1.0, Section 1 (AI Search
 * layer): "fast discovery assistant."
 *
 * Sits on [GlassSurface], not directly on the dark canvas — per the
 * two-surface text model, its label text uses the *original* light-system
 * token ([RojanTextSecondary]), not an on-dark token, since the local
 * surface under it is glass, regardless of the canvas around it.
 *
 * AI identity comes from a single [RojanAIGlow]-tinted sparkle icon, not
 * from any glow/background effect on the bar itself — kept deliberately
 * subtle so this doesn't visually compete with the Hero Booking Area
 * immediately below it, per the Design Board's explicit rule.
 *
 * [RojanShapes.Small] (a tighter radius than the Hero Card's
 * [RojanShapes.GlassCard]) is used here intentionally — a search bar
 * reads as an input control, not a hero-level surface, and should look
 * the part.
 *
 * Home Screen Production Pass, Task 6: vertical padding reduced from
 * [RojanDimens.SpaceMD] to [RojanDimens.SpaceSM] (horizontal unchanged)
 * — this bar's row was the only one of Home's glass surfaces using the
 * larger 16dp padding on every edge; every card elsewhere on this screen
 * uses 8dp. Both icons are now [RojanIconSize.Medium] (20dp, matching
 * the search icon's pre-existing size) instead of the sparkle's previous
 * bespoke 18dp, so the two icons read as the same visual weight.
 */
@Composable
fun AISearchBar(
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
        shape = RojanShapes.Small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceSM),
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RojanIconContainer(
    imageVector = Icons.Filled.Search,
    contentDescription = null,
    tint = RojanTextSecondary,
    size = RojanIconSize.Medium,
)

            Text(
                text = "جستجوی سالن، خدمات یا متخصص...",
                style = RojanTypography.Body.rojanPressedShadow(interactionSource),
                color = RojanTextSecondary,
                modifier = Modifier.weight(1f),
            )

            RojanIconContainer(
    imageVector = Icons.Filled.AutoAwesome,
    contentDescription = "دستیار هوشمند",
    tint = RojanAIGlow,
    size = RojanIconSize.Medium,
)
        }
    }
}
