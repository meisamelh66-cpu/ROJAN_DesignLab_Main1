package ai.rojan.designlab.components.brand

import androidx.compose.foundation.layout.Column
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import ai.rojan.designlab.ui.theme.RojanDarkSurfaceText
import ai.rojan.designlab.ui.theme.RojanMutedText


@Composable
fun VersionFooter(
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "ROJAN AI",
            color = RojanDarkSurfaceText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )

        // Global Design System Refinement (Phase 1): "Increase
        // readability slightly. Around 70% opacity. Maintain subtle
        // appearance." - a disclosed, deliberate exception to "never
        // reduce opacity of body text" since a version label is
        // explicitly meta/de-emphasized content, not body copy.
        Text(
            text = "Version 1.0.0",
            color = RojanMutedText.copy(alpha = 0.70f),
            fontSize = 11.sp
        )

    }
}