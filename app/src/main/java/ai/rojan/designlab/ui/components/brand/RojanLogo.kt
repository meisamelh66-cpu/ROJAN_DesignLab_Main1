package ai.rojan.designlab.components.brand

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextSecondary

/**
 * Global Design System Refinement (Phase 1) — Welcome Screen reference
 * implementation.
 *
 * Title: font size reduced 34sp -> 29sp (~15%, "Reduce logo size by
 * approximately 10-15%"), letter spacing increased 3sp -> 3.5sp
 * ("Improve letter spacing"), Bold weight kept unchanged.
 *
 * Subtitle: previously [RojanTextOnGlass] at 75% alpha ("excessive
 * transparency" per this same refinement's own framing) - now
 * [RojanTextSecondary], a real secondary-tier token at full opacity,
 * genuinely distinct from the title without an opacity trick. "Remove
 * excessive transparency. Use stronger text contrast."
 */
@Composable
fun RojanLogo(
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "ROJAN AI",
            color = RojanTextOnGlass,
            fontSize = 29.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.5.sp,
            textAlign = TextAlign.Center
        )


        Text(
            text = "اکوسیستم هوشمند زیبایی",
            color = RojanTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

    }
}