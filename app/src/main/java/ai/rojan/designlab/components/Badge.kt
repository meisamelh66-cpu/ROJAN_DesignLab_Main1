package ai.rojan.designlab.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.rojan.designlab.ui.theme.RojanButtonText
import ai.rojan.designlab.ui.theme.RojanVividPurple

/**
 * Small reusable pill badge — used for the "AI" tag under the header logo
 * and the "پیشنهادترین" (Most Recommended) tag on the Hero Card.
 *
 * Reusable anywhere a short tag/label needs a glassy accent pill.
 */
@Composable
fun Badge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = RojanVividPurple,
    contentColor: Color = RojanButtonText,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
            )
        }
        Text(
            text = text,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAF8F6)
@Composable
private fun BadgePreview() {
    Badge(text = "AI")
}

@Preview(showBackground = true, backgroundColor = 0xFFFAF8F6, name = "Recommended Badge")
@Composable
private fun BadgeRecommendedPreview() {
    Badge(text = "پیشنهادترین")
}