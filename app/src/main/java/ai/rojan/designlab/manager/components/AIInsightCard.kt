package ai.rojan.designlab.manager.components

import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Manager App workspace — AI insight card. Static placeholder copy only
 * ("No backend" per this pass).
 *
 * ROJAN AI Manager Visual Theme Implementation: re-themed for the dark
 * luxury background ([ManagerGlassSurface]/[ManagerIconContainer], Gold
 * accent for the AI signal) — content/copy unchanged.
 */
@Composable
fun AIInsightCard(modifier: Modifier = Modifier) {
    ManagerGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RojanShapes.GlassCard,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            ManagerIconContainer(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "پیشنهاد هوش مصنوعی",
                containerSize = 56.dp,
                accentColor = ManagerColors.Gold,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "پیشنهاد هوش مصنوعی",
                    style = RojanTypography.CardTitle,
                    color = ManagerColors.TextPrimary,
                )
                Text(
                    text = "امروز بین ساعت ۱۶ تا ۱۸ سه نوبت خالی دارید. "
                        + "برای پر کردن آن‌ها می‌توانید به مشتریان اخیر پیام ارسال کنید.",
                    style = RojanTypography.Body,
                    color = ManagerColors.TextSecondary,
                    modifier = Modifier.padding(top = RojanDimens.SpaceXS),
                )
            }
        }
    }
}
