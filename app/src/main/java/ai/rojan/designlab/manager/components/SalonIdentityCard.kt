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
 * Manager App workspace — salon identity summary, rendered from the
 * frozen-then-updated Manager theme ([ManagerGlassSurface],
 * [ManagerIconContainer], [ManagerColors]) — no Customer-facing
 * component reused.
 *
 * Phase 2, M6: [salonName]/[salonCategory]/[isActive] are real backend
 * fields (`GET /salons/{salonId}`'s `name`/`description`/`active`) once
 * [ai.rojan.designlab.manager.data.ManagerRepositories.salon] loads.
 * [salonCategory] is the salon's free-text `description`; the backend has
 * no distinct "category" field.
 *
 * Manager Dashboard Active Salon Fix: [isLoading] replaces the previous
 * hardcoded preview-only defaults (`سالن رویان` etc.) that used to stand
 * in for "not loaded yet" — those were silently indistinguishable from a
 * real salon named the same, which was the exact bug in
 * `ROJAN_Active_Salon_Context_Root_Cause_Report_v1.md`. Callers must now
 * pass real data or explicitly ask for the loading state; there is no
 * fallback that looks like a real salon.
 *
 * ROJAN AI Manager Visual Theme Implementation: re-themed for the dark
 * luxury background — content/layout unchanged.
 */
@Composable
fun SalonIdentityCard(
    modifier: Modifier = Modifier,
    salonName: String = "",
    salonCategory: String? = null,
    isActive: Boolean = true,
    isLoading: Boolean = false,
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
                contentDescription = if (isLoading) "در حال بارگذاری سالن" else salonName,
                containerSize = 64.dp,
            )

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
}
