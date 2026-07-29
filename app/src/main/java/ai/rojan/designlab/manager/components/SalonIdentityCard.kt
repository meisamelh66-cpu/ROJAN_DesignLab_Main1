package ai.rojan.designlab.manager.components

import ai.rojan.designlab.ui.components.effects.RojanAmbientGlow
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanGlassText
import ai.rojan.designlab.ui.theme.RojanShadows
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanStatusOnline
import ai.rojan.designlab.ui.theme.RojanTextOnDarkSurface
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Manager App workspace — salon identity summary. Static placeholder
 * content (salon name/status), rendered entirely from shared primitives
 * ([GlassSurface], [RojanAmbientGlow], [RojanTypography]/tokens) — no
 * backend, no Customer-facing component reused.
 */
@Composable
fun SalonIdentityCard(
    modifier: Modifier = Modifier,
    salonName: String = "سالن رویان",
    salonCategory: String = "آرایش و زیبایی بانوان",
    isActive: Boolean = true,
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RojanShapes.GlassCard,
        elevation = RojanShadows.FloatingElevation,
        glassAlpha = ManagerGlass.Alpha,
        glassSecondaryAlpha = ManagerGlass.SecondaryAlpha,
        borderAlpha = ManagerGlass.BorderAlpha,
        borderSecondaryAlpha = ManagerGlass.BorderSecondaryAlpha,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            Box(contentAlignment = Alignment.Center) {
                RojanAmbientGlow(
                    modifier = Modifier.size(72.dp),
                    color = ManagerAccent.Teal,
                    alpha = 0.15f,
                )
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(ManagerAccent.Teal.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    RojanIconContainer(
                        imageVector = Icons.Filled.Storefront,
                        contentDescription = salonName,
                        size = RojanIconSize.Large,
                        tint = RojanGlassText,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = salonName,
                    style = RojanTypography.CardTitle,
                    color = RojanGlassText,
                )
                Text(
                    text = salonCategory,
                    style = RojanTypography.Caption,
                    color = RojanTextOnDarkSurface,
                    modifier = Modifier.padding(top = RojanDimens.SpaceXS),
                )

                Row(
                    modifier = Modifier.padding(top = RojanDimens.SpaceSM),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (isActive) RojanStatusOnline else RojanTextOnDarkSurface,
                                CircleShape,
                            ),
                    )
                    Text(
                        text = if (isActive) "فعال" else "غیرفعال",
                        style = RojanTypography.Caption,
                        color = if (isActive) RojanStatusOnline else RojanTextOnDarkSurface,
                    )
                }
            }
        }
    }
}
