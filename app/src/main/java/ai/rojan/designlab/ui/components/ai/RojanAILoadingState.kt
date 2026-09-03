package ai.rojan.designlab.ui.components.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import ai.rojan.designlab.ui.motion.RojanAiThinkingIndicator
import ai.rojan.designlab.ui.motion.rememberReducedMotion
import ai.rojan.designlab.ui.motion.rojanAiGlow
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.LocalRojanPalette
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * ROJAN AI loading treatment (UI Polish Sprint 4, Task 5) — the one calm
 * "AI is working" indicator for inline use, so every AI-driven wait in
 * the app looks the same.
 *
 * Composes the two Sprint 2 primitives — a very faint breathing brand
 * glow ([rojanAiGlow]) behind the calm three-dot [RojanAiThinkingIndicator]
 * — plus a short label. No spinner, no bar, no "estimating 60%". Under
 * reduced motion the glow holds still and the dots render static — the
 * label alone still says "working".
 *
 * For an AI *card's* loading state, use `RojanAISurface(state = Loading)`
 * (which shows the same indicator inside the glass). This standalone
 * version is for a header row, a section, or a sheet.
 */
@Composable
fun RojanAILoadingState(
    modifier: Modifier = Modifier,
    label: String = "در حال بررسی…",
    accentColor: Color = LocalRojanPalette.current.textAccent,
) {
    val palette = LocalRojanPalette.current
    val reduceMotion = rememberReducedMotion()

    Row(
        modifier = modifier
            .rojanAiGlow(color = accentColor, reduceMotion = reduceMotion, baseAlpha = 0.05f, pulseDelta = 0.03f)
            .padding(RojanDimens.SpaceSM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        RojanAiThinkingIndicator(color = accentColor, reduceMotion = reduceMotion)
        Text(text = label, style = RojanTypography.Body, color = palette.textSecondary)
    }
}
