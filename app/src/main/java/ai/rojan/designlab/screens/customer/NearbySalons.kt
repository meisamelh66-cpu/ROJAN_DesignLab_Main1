package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.components.icon.RojanIconContainer

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
 * call is unaffected) — [ai.rojan.designlab.screens.booking.BookingLandingScreen]
 * previously called a since-deleted `NearbySalonsSection` composable
 * with its own separate click handling; rather than recreating that
 * deleted component (a duplicate) or restoring it (dead code), this is
 * the single shared implementation both screens now use.
 */
@Composable
fun NearbySalons(onSalonClick: ((String) -> Unit)? = null) {
    val catalogEngine = remember { CatalogEngine() }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        items(catalogEngine.allSalons()) { salon ->
            Box(
                modifier = Modifier
                    .size(width = 160.dp, height = 190.dp)
                    .background(salon.colorSeed.copy(alpha = 0.30f), RojanShapes.Small)
            ) {
                GlassSurface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onSalonClick?.invoke(salon.id) },
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
                                Image(
                                    painter = painterResource(id = salon.assetRes),
                                    contentDescription = salon.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                RojanIconContainer(
    imageVector = Icons.Filled.Storefront,
    contentDescription = null,
    tint = RojanTextPrimary,
    sizeOverride = 32.dp,
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
    imageVector = Icons.Filled.LocationOn,
    contentDescription = "فاصله",
    tint = RojanTextSecondary,
    sizeOverride = 14.dp,
)
                            Text(
                                text = "${salon.distanceKm} کیلومتر",
                                style = RojanTypography.Caption,
                                color = RojanTextSecondary,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                        ) {
                            RojanIconContainer(
    imageVector = Icons.Filled.Star,
    contentDescription = "امتیاز",
    tint = RojanTextSecondary,
    sizeOverride = 14.dp,
)
                            Text(
                                text = salon.rating,
                                style = RojanTypography.Caption,
                                color = RojanTextSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}
