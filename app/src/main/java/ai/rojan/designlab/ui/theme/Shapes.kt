package ai.rojan.designlab.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape


/**
 * Compose [androidx.compose.ui.graphics.Shape]s for the ROJAN design
 * system. Every value derives from [RojanRadius] — the single radius
 * source of truth — so a corner-radius change happens in exactly one
 * place.
 *
 * Names and rendered corners are unchanged from before [RojanRadius]
 * existed; this only replaces the duplicated raw `.dp` literals with the
 * named tier they always equalled (`GlassCard` = `Card` 32, `Small` = 16,
 * `PremiumButton` = `Pill` 50, `Circle` = 100).
 */
object RojanShapes {

    val GlassCard = RoundedCornerShape(RojanRadius.Card)

    val PremiumButton = RoundedCornerShape(RojanRadius.Pill)

    val Circle = RoundedCornerShape(RojanRadius.Circle)

    val Small = RoundedCornerShape(RojanRadius.Small)
}
