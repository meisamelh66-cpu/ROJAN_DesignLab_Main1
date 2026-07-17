package ai.rojan.designlab.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanAquaMint
import ai.rojan.designlab.ui.theme.RojanBlushPink
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShadows
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanSoftLavender
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.components.icon.RojanIconContainer

/**
 * Booking Module Design System Migration.
 *
 * The card's original 3-stop pastel fill (`#E8D9FF`/`#DDF7FF`/`#FFE4F1`)
 * maps onto the existing [RojanSoftLavender]/[RojanAquaMint]/
 * [RojanBlushPink] triplet — same pastel-tint-under-glass family already
 * used throughout Customer Home, not a new visual concept. Unlike
 * Customer Home's own `HeroBookingCard` (which had its fill deliberately
 * removed per an explicit later decision), this card's fill is
 * preserved — no equivalent decision was made for Booking, and
 * "preserve appearance as closely as possible" governs this migration.
 */
@Composable
fun BookingHeroCard(
    onBookClick: () -> Unit = {},
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = RojanShadows.PremiumElevation,
                shape = RojanShapes.GlassCard
            ),
        shape = RojanShapes.GlassCard
    ) {

        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            RojanSoftLavender,
                            RojanAquaMint,
                            RojanBlushPink
                        )
                    )
                )
                .padding(RojanDimens.SpaceLG)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        text = "✨ رزرو هوشمند زیبایی",
                        style = RojanTypography.HeroTitle,
                        color = RojanTextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "بهترین سالن و متخصص را با AI پیدا کنید",
                        style = RojanTypography.Body,
                        color = RojanTextSecondary
                    )

                    Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))

                    Button(
                        onClick = onBookClick,
                        shape = RojanShapes.PremiumButton,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RojanAIGlow
                        )
                    ) {
                        Text(
                            text = "شروع رزرو",
                            style = RojanTypography.Button
                        )
                    }
                }

                Spacer(modifier = Modifier.width(RojanDimens.SpaceSM))

                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = RojanShapes.Small,
                    color = RojanTextOnGlass.copy(alpha = 0.45f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        RojanIconContainer(
    imageVector = Icons.Default.AutoAwesome,
    contentDescription = "AI",
    tint = RojanAIGlow,
    sizeOverride = 45.dp,
)
                    }
                }
            }
        }
    }
}
