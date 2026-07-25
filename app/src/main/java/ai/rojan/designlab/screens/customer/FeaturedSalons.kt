package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.image.RojanSampleImage
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize

/**
 * Customer Home featured salons — Design Board v1.0, Secondary Features
 * layer. Purpose is trust-building and inspiration, explicitly not the
 * main booking action — visual priority sits below HeroBookingCard and
 * AISearchBar, above Recommendations/History sections, per the Board.
 *
 * Architecture Cleanup Sprint (Task 3): data now comes from
 * [ai.rojan.designlab.data.demo.DemoSalonRepository] via [CatalogEngine]
 * — the same canonical salon list Journey 1's Search/Salon Details
 * already use, not a separate local dataset with slightly different
 * wording for the same 4 salons. `DemoSalon` gained an `assetRes` field
 * to preserve this widget's real-asset-image capability exactly as it
 * was.
 *
 * Home Screen Production Pass, Task 7/8: card size moved from a raw
 * `160.dp`/`190.dp` pair onto [RojanDimens.CardWidthStandard]/
 * [RojanDimens.CardHeightStandard] — the token that already existed for
 * exactly this value (see its own doc comment); zero visual change,
 * removes a magic-number duplicate of the token.
 *
 * Production Asset Normalization: the photo now renders through
 * [RojanSampleImage] instead of a raw `Image` — same visual result, but
 * the image is now actually clipped to its own shape (it wasn't before;
 * only the background behind it was) and the crop/loading behavior is
 * shared with every other sample-image card in the app.
 */
@Composable
fun FeaturedSalons() {
    val catalogEngine = remember { CatalogEngine() }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        items(catalogEngine.allSalons()) { salon ->
            Box(
                modifier = Modifier
                    .size(width = RojanDimens.CardWidthStandard, height = RojanDimens.CardHeightStandard)
                    // Visual Refinement: tint reduced 0.30f -> 0.25f (~17%). No
                    // literal "dark overlay on the salon photo" exists in this
                    // component (the Image draws opaque, fully covering this
                    // background) - this is the closest real, honest mapping of
                    // "reduce overlay / clearer image" to something that
                    // actually affects this card's appearance.
                    .background(salon.colorSeed.copy(alpha = 0.25f), RojanShapes.Small)
            ) {
                GlassSurface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { },
                    shape = RojanShapes.Small,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RojanDimens.SpaceSM),
                        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
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
    tint = RojanTextPrimary,
    size = RojanIconSize.Large,
)
                            }
                        }

                        Text(
                            text = salon.name,
                            style = RojanTypography.Caption,
                            color = RojanTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                        ) {
                            RojanIconContainer(
    imageVector = Icons.Filled.Star,
    contentDescription = "امتیاز",
    tint = RojanTextSecondary,
    size = RojanIconSize.Small,
)
                            Text(
                                text = salon.rating,
                                style = RojanTypography.Caption,
                                color = RojanTextSecondary,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                        ) {
                            RojanIconContainer(
    imageVector = Icons.Filled.LocationOn,
    contentDescription = null,
    tint = RojanTextSecondary,
    size = RojanIconSize.Small,
)
                            Text(
                                text = salon.tagline,
                                style = RojanTypography.Caption,
                                color = RojanTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
