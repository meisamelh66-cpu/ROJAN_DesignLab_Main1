package ai.rojan.designlab.manager.components

import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanStatusOnline
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
 * content (salon name/status), rendered from the frozen-then-updated
 * Manager theme ([ManagerGlassSurface], [ManagerIconContainer],
 * [ManagerColors]) — no backend, no Customer-facing component reused.
 *
 * ROJAN AI Manager Visual Theme Implementation: re-themed for the dark
 * luxury background — content/layout unchanged.
 */
@Composable
fun SalonIdentityCard(
    modifier: Modifier = Modifier,
    salonName: String = "سالن رویان",
    salonCategory: String = "آرایش و زیبایی بانوان",
    isActive: Boolean = true,
) {
    ManagerGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RojanShapes.GlassCard,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            ManagerIconContainer(
                imageVector = Icons.Filled.Storefront,
                contentDescription = salonName,
                containerSize = 64.dp,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = salonName,
                    style = RojanTypography.CardTitle,
                    color = ManagerColors.TextPrimary,
                )
                Text(
                    text = salonCategory,
                    style = RojanTypography.Caption,
                    color = ManagerColors.TextSecondary,
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
                                if (isActive) RojanStatusOnline else ManagerColors.TextSecondary,
                                CircleShape,
                            ),
                    )
                    Text(
                        text = if (isActive) "فعال" else "غیرفعال",
                        style = RojanTypography.Caption,
                        color = if (isActive) RojanStatusOnline else ManagerColors.TextSecondary,
                    )
                }
            }
        }
    }
}
