package ai.rojan.designlab.manager.components

import ai.rojan.designlab.ui.components.icon.PremiumIconContainer
import ai.rojan.designlab.ui.components.icon.PremiumIconContainerDefaults
import ai.rojan.designlab.ui.components.icon.RojanIconContainerStyle
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp

/**
 * ROJAN Manager icon theme — thin backward-compatible wrapper.
 *
 * Design-system refinement, Phase 4 (Unified Premium Icon Container): the
 * circular glow+glass container this component used to own directly
 * (soft ambient glow behind a circular glass surface, accent-tinted
 * gradient fill, accent→gold gradient rim) now lives once in the shared
 * [PremiumIconContainer] as [RojanIconContainerStyle.Glass] — this call
 * reproduces the pre-unification look for Manager's two accents exactly
 * (see [PremiumIconContainer]'s own doc comment for the one, effectively
 * imperceptible, generalization). Kept as a named wrapper — not deleted
 * with all ~20 call sites migrated — purely so no Manager screen needs to
 * change.
 *
 * [accentColor] still defaults to Turquoise (the primary Manager accent)
 * and stays freely overridable per call site — the reference design
 * alternates teal-tinted and gold-tinted icon circles across the KPI row
 * (booking/customers vs. revenue/occupancy), preserved exactly.
 */
object ManagerIconTheme {
    val ContainerSize: Dp = PremiumIconContainerDefaults.ContainerSize
    val GlowSizeMultiplier: Float = PremiumIconContainerDefaults.GlowSizeMultiplier
}

@Composable
fun ManagerIconContainer(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: RojanIconSize = RojanIconSize.Medium,
    containerSize: Dp = ManagerIconTheme.ContainerSize,
    accentColor: Color = ManagerColors.Turquoise,
) {
    PremiumIconContainer(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        size = size,
        containerSize = containerSize,
        style = RojanIconContainerStyle.Glass,
        accentColor = accentColor,
        // Manager's original rule: the glyph itself gets the accent's
        // lighter variant, distinct from the darker tone driving the
        // glow/fill/border — preserved exactly.
        iconTint = if (accentColor == ManagerColors.Gold) ManagerColors.GoldLight else ManagerColors.TurquoiseLight,
    )
}
