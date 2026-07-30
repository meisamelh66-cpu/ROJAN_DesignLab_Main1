package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Storefront
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.screens.customer.hometheme.HomeCard
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeRatingRow
import ai.rojan.designlab.ui.components.image.RojanSampleImage
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize

/**
 * Customer Home nearby salons — Design Board v1.0, Secondary Features
 * layer.
 *
 * Code Cleanup pass: migrated off its previous local `fakeNearbySalons`
 * list onto [CatalogEngine.allSalons] — the same canonical source
 * [FeaturedSalons] already uses. Real [ai.rojan.designlab.data.demo.DemoSalon.distanceKm]
 * used for distance (already existed, was never wired here) instead of
 * a separate mock distance string.
 * Root Cause Build Analysis: [onSalonClick] added (optional, defaults
 * to `null` so Customer Home's existing parameterless `NearbySalons()`
 * call is unaffected).
 *
 * Home Screen Production Pass, Task 7/8: card size moved from a raw
 * `160.dp`/`190.dp` pair onto [RojanDimens.CardWidthStandard]/
 * [RojanDimens.CardHeightStandard] — zero visual change, see
 * [FeaturedSalons]'s identical note.
 *
 * UX Refactor Phase 1: previously also shared with `BookingLandingScreen`
 * (the old, duplicate "Journey 1 landing" screen), which was deleted —
 * Customer Home is now the single canonical landing screen.
 */
@Composable
fun NearbySalons(onSalonClick: ((String) -> Unit)? = null) {
    val catalogEngine = remember { CatalogEngine() }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        itemsIndexed(catalogEngine.allSalons()) { index, salon ->
            HomeCard(
                accentColor = salon.colorSeed,
                onClick = { onSalonClick?.invoke(salon.id) },
                index = index,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(salon.colorSeed.copy(alpha = 0.5f), RojanShapes.Small),
                    contentAlignment = Alignment.Center,
                ) {
                    if (salon.assetRes != null) {
                        RojanSampleImage(
                            resId = salon.assetRes,
                            contentDescription = salon.name,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        RojanIconContainer(
                            imageVector = Icons.Filled.Storefront,
                            contentDescription = null,
                            tint = HomeColors.TextPrimary,
                            size = RojanIconSize.Large,
                        )
                    }
                }

                Text(
                    text = salon.name,
                    style = RojanTypography.Caption,
                    color = HomeColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                ) {
                    RojanIconContainer(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "فاصله",
                        tint = HomeColors.TextSecondary,
                        size = RojanIconSize.Small,
                    )
                    Text(
                        text = "${salon.distanceKm} کیلومتر",
                        style = RojanTypography.Caption,
                        color = HomeColors.TextSecondary,
                    )
                }

                HomeRatingRow(rating = salon.rating)
            }
        }
    }
}
