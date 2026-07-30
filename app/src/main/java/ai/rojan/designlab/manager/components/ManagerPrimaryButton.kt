package ai.rojan.designlab.manager.components

import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

/**
 * Manager Booking Journey Phase 2 — a small, reusable primary-CTA
 * wrapper built only from existing Manager theme primitives
 * ([ManagerGlassSurface]/[ManagerColors]), so the 5 booking screens that
 * need a "continue"/"confirm" action don't each hand-roll a near-
 * identical button. Not a visual-theme change — same fill/border/shadow
 * language every Manager chip already uses.
 */
@Composable
fun ManagerPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ManagerGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .let { if (enabled) it.rojanPressable(onClick = onClick) else it },
        shape = RojanShapes.PremiumButton,
        fillAlpha = ManagerGlassTheme.FillAlpha * 2f,
        borderAlpha = ManagerGlassTheme.BorderAlpha,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RojanDimens.SpaceMD),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = text, style = RojanTypography.Button, color = ManagerColors.Gold)
        }
    }
}
