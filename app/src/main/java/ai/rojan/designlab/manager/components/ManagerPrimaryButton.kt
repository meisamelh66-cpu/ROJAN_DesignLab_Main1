package ai.rojan.designlab.manager.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.theme.RojanButtonStyle

/**
 * ROJAN Manager primary CTA — thin backward-compatible wrapper.
 *
 * Design-system refinement, Phase 3 (Unified Premium Button System): the
 * glass-CTA rendering this component used to own directly (Manager
 * palette colors, the Teal->Gold wash, the loading/disabled states) now
 * lives once in the shared [PremiumButton] as [RojanButtonStyle.Glass] —
 * which is exactly what [ai.rojan.designlab.ui.theme.ManagerPalette.buttonStyle]
 * resolves to, so this call is pixel-identical to the pre-unification
 * `ManagerPrimaryButton` body. Kept as a named wrapper (rather than
 * deleting it and touching all 15 call sites) purely so no Manager screen
 * needs to change — `fillMaxWidth()` is applied here exactly as before,
 * not left to the caller.
 */
@Composable
fun ManagerPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    PremiumButton(
        text = text,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        loading = loading,
        style = RojanButtonStyle.Glass,
    )
}
