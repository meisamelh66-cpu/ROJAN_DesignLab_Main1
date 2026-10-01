package ai.rojan.designlab.manager.components

import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanStatusOnline
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Manager App workspace — salon identity summary.
 *
 * Phase 2, M6: [salonName]/[salonCategory]/[isActive] are real backend
 * fields (`GET /salons/{salonId}`'s `name`/`description`/`active`).
 * [salonCategory] is the salon's free-text `description`; the backend has
 * no distinct "category" field.
 *
 * Master Integration Repair, PASS 2 (doc correction): this doc comment
 * previously cited [ai.rojan.designlab.manager.data.ManagerRepositories.salon]
 * as the data source — stale since TEAM2-002 (Manager Data Persistence)
 * moved this card onto
 * [ai.rojan.designlab.manager.presentation.dashboard.ManagerDashboardViewModel]
 * (a real, fresh `GET /api/v1/salons/mine` fetch on every screen entry),
 * which is the actual, current data source. Investigated as part of this
 * repair pass: the master integration audit's "Manager's own Dashboard
 * salon card goes stale after a Salon-Setup edit" finding no longer holds
 * against current source — this card's caller (`ManagerDashboardScreen`)
 * does not read `ManagerRepositories.salon` at all, so a stale value there
 * cannot affect it. No functional change was needed here beyond this
 * correction.
 *
 * Manager Dashboard Active Salon Fix: [isLoading] replaces the previous
 * hardcoded preview-only defaults (`سالن رویان` etc.) that used to stand
 * in for "not loaded yet" — those were silently indistinguishable from a
 * real salon named the same. Callers must now pass real data or
 * explicitly ask for the loading state; there is no fallback that looks
 * like a real salon.
 *
 * Safe-redesign pass: a flat panel (plain neutral shadow, no
 * [ManagerGlassSurface]) plus a solid — not translucent — Turquoise
 * avatar circle gives this card its own visual identity through color
 * rather than another metallic-border glass surface. Every modifier here
 * is a fixed positive dp value; no size-dependent or negative offsets.
 */
@Composable
fun SalonIdentityCard(
    modifier: Modifier = Modifier,
    salonName: String = "",
    salonCategory: String? = null,
    isActive: Boolean = true,
    isLoading: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RojanShapes.GlassCard,
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color.Black.copy(alpha = 0.3f),
            )
            .background(ManagerColors.BaseSecondary.copy(alpha = 0.45f), RojanShapes.GlassCard)
            .border(1.dp, ManagerColors.Turquoise.copy(alpha = 0.2f), RojanShapes.GlassCard)
            .padding(RojanDimens.SpaceMD),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        // Pixel-perfect pass: a thin turquoise ring (not a solid fill) —
        // matches the reference's avatar treatment. No photo asset exists
        // for the salon, so the icon fallback stays honest rather than a
        // fabricated image.
        Box(
            modifier = Modifier
                .size(56.dp)
                .border(1.5.dp, ManagerColors.Turquoise, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            RojanIconContainer(
                imageVector = Icons.Filled.Storefront,
                contentDescription = if (isLoading) "در حال بارگذاری سالن" else salonName,
                tint = ManagerColors.Turquoise,
                size = RojanIconSize.Large,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isLoading) "در حال بارگذاری..." else salonName,
                style = RojanTypography.CardTitle,
                color = if (isLoading) ManagerColors.TextSecondary else ManagerColors.TextPrimary,
            )
            if (!isLoading && !salonCategory.isNullOrBlank()) {
                Text(
                    text = salonCategory,
                    style = RojanTypography.Caption,
                    color = ManagerColors.TextSecondary,
                    modifier = Modifier.padding(top = RojanDimens.SpaceXS),
                )
            }

            if (!isLoading) {
                val statusColor = if (isActive) RojanStatusOnline else ManagerColors.TextSecondary
                // Pixel-perfect pass: plain dot + text, no pill/badge
                // background — matches the reference's simpler status
                // treatment.
                Row(
                    modifier = Modifier.padding(top = RojanDimens.SpaceSM),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape),
                    )
                    Text(
                        text = if (isActive) "فعال" else "غیرفعال",
                        style = RojanTypography.Caption,
                        color = statusColor,
                    )
                }
            }
        }

        // Pixel-perfect pass: visual-only trailing chevron, matching the
        // reference — no destination exists for "salon details" today, so
        // this carries no click handler and makes no navigation promise.
        if (!isLoading) {
            RojanIconContainer(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = null,
                tint = ManagerColors.TextSecondary,
                size = RojanIconSize.Medium,
            )
        }
    }
}
