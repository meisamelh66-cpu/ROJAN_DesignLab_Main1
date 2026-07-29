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
import ai.rojan.designlab.ui.theme.RojanTextOnDarkSurface
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Manager App workspace — AI insight card. Static placeholder copy only
 * ("No backend" per this pass) — same quiet ambient-glow-behind-icon
 * treatment [ai.rojan.designlab.components.hero.HeroBookingCard] already
 * uses for its own AI-active accent, reused here rather than a new
 * effect invented for Manager.
 */
@Composable
fun AIInsightCard(modifier: Modifier = Modifier) {
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
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            Box(contentAlignment = Alignment.Center) {
                RojanAmbientGlow(
                    modifier = Modifier.size(64.dp),
                    color = ManagerAccent.Gold,
                    alpha = 0.18f,
                )
                RojanIconContainer(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "پیشنهاد هوش مصنوعی",
                    size = RojanIconSize.Large,
                    tint = ManagerAccent.Gold,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "پیشنهاد هوش مصنوعی",
                    style = RojanTypography.CardTitle,
                    color = RojanGlassText,
                )
                Text(
                    text = "امروز بین ساعت ۱۶ تا ۱۸ سه نوبت خالی دارید. "
                        + "برای پر کردن آن‌ها می‌توانید به مشتریان اخیر پیام ارسال کنید.",
                    style = RojanTypography.Body,
                    color = RojanTextOnDarkSurface,
                    modifier = Modifier.padding(top = RojanDimens.SpaceXS),
                )
            }
        }
    }
}
