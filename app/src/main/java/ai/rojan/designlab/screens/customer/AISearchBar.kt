package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.components.icon.RojanIconContainer

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
 */
@Composable
fun AISearchBar(
    onClick: () -> Unit = {},
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RojanShapes.Small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RojanIconContainer(
    imageVector = Icons.Filled.Search,
    contentDescription = null,
    tint = RojanTextSecondary,
    sizeOverride = 20.dp,
)

            Text(
                text = "جستجوی سالن، خدمات یا متخصص...",
                style = RojanTypography.Body,
                color = RojanTextSecondary,
                modifier = Modifier.weight(1f),
            )

            RojanIconContainer(
    imageVector = Icons.Filled.AutoAwesome,
    contentDescription = "دستیار هوشمند",
    tint = RojanAIGlow,
    sizeOverride = 18.dp,
)
        }
    }
}
