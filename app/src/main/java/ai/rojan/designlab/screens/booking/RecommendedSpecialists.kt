package ai.rojan.designlab.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.data.demo.DemoSpecialist
import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanAvatarGradientStart
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanGlassWhite
import ai.rojan.designlab.ui.theme.RojanRatingGold
import ai.rojan.designlab.ui.theme.RojanShadows
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanStatusOnline
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.components.icon.RojanIconContainer

/**
 * Journey 1 completion: now driven by [DemoSpecialistRepository] instead
 * of 2 hardcoded name/service strings — real IDs are what make
 * `onSpecialistClick` navigation to Specialist Profile possible at all.
 */
@Composable
fun RecommendedSpecialists(
    onSpecialistClick: (String) -> Unit,
) {
    val catalogEngine = remember { CatalogEngine() }
    Column(verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD)) {

        Text(
            text = "⭐ متخصصان پیشنهادی",
            fontSize = RojanDimens.SubtitleSize,
            color = RojanTextPrimary
        )

        catalogEngine.allSpecialists().take(2).forEach { specialist ->
            SpecialistPremiumCard(
                specialist = specialist,
                onClick = { onSpecialistClick(specialist.id) },
            )
        }
    }
}

@Composable
private fun SpecialistPremiumCard(
    specialist: DemoSpecialist,
    onClick: () -> Unit,
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = RojanShadows.PremiumElevation,
                shape = RojanShapes.GlassCard
            )
            .clickable(onClick = onClick),
        shape = RojanShapes.GlassCard
    ) {

        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            RojanGlassWhite.copy(alpha = 0.95f),
                            specialist.colorSeed,
                        )
                    )
                )
                .padding(22.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = RojanTextOnGlass,
                    modifier = Modifier
                        .size(74.dp)
                        .shadow(
                            elevation = RojanShadows.FloatingElevation,
                            shape = CircleShape
                        )
                        .background(
                            Brush.radialGradient(
                                colors = listOf(RojanAvatarGradientStart, RojanAIGlow)
                            ),
                            CircleShape
                        )
                        .padding(18.dp)
                )

                Column(horizontalAlignment = Alignment.End) {

                    Text(
                        text = specialist.name,
                        fontSize = RojanDimens.SubtitleSize,
                        color = RojanTextPrimary
                    )

                    Spacer(modifier = Modifier.height(RojanDimens.SpaceXS))

                    Text(
                        text = specialist.title,
                        style = RojanTypography.Body,
                        color = RojanTextSecondary
                    )

                    Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Text(
                            text = "🟢 آنلاین",
                            style = RojanTypography.Caption,
                            color = RojanStatusOnline
                        )

                        Spacer(modifier = Modifier.size(RojanDimens.SpaceSM))

                        RojanIconContainer(
    imageVector = Icons.Default.Star,
    contentDescription = null,
    tint = RojanRatingGold,
    sizeOverride = 18.dp,
)

                        Text(text = specialist.rating, color = RojanAIGlow)
                    }
                }
            }

            Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RojanShapes.PremiumButton,
                colors = ButtonDefaults.buttonColors(containerColor = RojanAIGlow)
            ) {
                Text(text = "مشاهده و رزرو", style = RojanTypography.Button)
            }
        }
    }
}
