package ai.rojan.designlab.manager.components

import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * ROJAN Manager primary CTA (UI Polish Sprint 3, Task 2 — "Manager CTA
 * should feel enterprise-grade").
 *
 * Still the shared Premium-Glass language ([ManagerGlassSurface] +
 * [ManagerColors], the metallic border), but now reads as a **filled**
 * action rather than a translucent chip: a Teal→Gold gradient wash sits
 * inside the glass (the two Manager accents, at low alpha — no new
 * colour), and the label is Gold. Weight that matches "confirm booking".
 *
 * States (Task 2):
 * - **pressed** — `rojanPressable` (shared press feedback), unchanged;
 * - **loading** — a spinner replaces the label at the same height, click
 *   blocked, gradient dropped so it clearly reads as "working, not ready";
 * - **disabled** — flat (no gradient), 45% opacity, click blocked — a
 *   distinct state, not just "a bit dimmer";
 * - **accessibility** — `role = Button`, `disabled()` when not
 *   interactive, and a Persian `stateDescription` while loading.
 *
 * Fixed `heightIn(min = 56.dp)` — comfortably above the 48dp touch
 * minimum and stable across the label⇄spinner swap.
 */
@Composable
fun ManagerPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val interactive = enabled && !loading

    ManagerGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .semantics {
                role = Role.Button
                if (!interactive) disabled()
                if (loading) stateDescription = "در حال پردازش"
            }
            .let { if (interactive) it.rojanPressable(onClick = onClick) else it },
        shape = RojanShapes.PremiumButton,
        fillAlpha = ManagerGlassTheme.FillAlpha * 2f,
        borderAlpha = ManagerGlassTheme.BorderAlpha,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .then(
                    if (interactive) {
                        Modifier.background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    ManagerColors.Turquoise.copy(alpha = 0.20f),
                                    ManagerColors.Gold.copy(alpha = 0.16f),
                                ),
                            ),
                            shape = RojanShapes.PremiumButton,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(vertical = RojanDimens.SpaceMD),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = ManagerColors.Gold,
                    strokeWidth = 2.5.dp,
                )
            } else {
                Text(
                    text = text,
                    style = RojanTypography.Button,
                    color = if (interactive) ManagerColors.Gold else ManagerColors.TextSecondary,
                )
            }
        }
    }
}
