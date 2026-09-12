package ai.rojan.designlab.screens.customer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.theme.RojanDimens

/* =============================================================================
 * ROJAN Customer — multi-step progress hairline.
 *
 * A quiet, editorial progress indicator for the booking journey (متخصص · خدمت ·
 * تاریخ · ساعت · تایید). Screens outside a multi-step flow simply don't render
 * it. No design-system change, no logic — a pure presentation of the
 * caller-supplied [step] / [totalSteps].
 * ========================================================================== */

private val SegmentShape = RoundedCornerShape(2.dp)

/**
 * [step] segments (1-based, clamped to `1..totalSteps`) are filled rose-gold;
 * the rest are a 9%-white hairline. 3dp tall, 4dp gaps, on the screen margin.
 */
@Composable
fun CustomerStepIndicator(
    step: Int,
    modifier: Modifier = Modifier,
    totalSteps: Int = 5,
) {
    val filled = step.coerceIn(1, totalSteps)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CustomerScreenMargin, vertical = RojanDimens.SpaceSM)
            .clearAndSetSemantics { stateDescription = "مرحله $filled از $totalSteps" },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(totalSteps) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(SegmentShape)
                    .background(if (index < filled) CustomerAccent else CustomerHairline),
            )
        }
    }
}
