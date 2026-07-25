package ai.rojan.designlab.screens.booking

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.data.demo.DemoSalon
import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.ui.background.PremiumBackground
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.image.RojanSampleImage
import ai.rojan.designlab.ui.components.interaction.rojanPressedShadow
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanRatingGold
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanLuxurySecondaryBody
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Booking Experience Refactor, spec section 9 — Salon Cards.
 * "Each card contains ONLY: Cover Image, Salon Name, Rating, Distance.
 * Do NOT add: Price, Address, Description, Opening hours, Phone,
 * Availability, Services." Enforced by construction — this composable
 * has no parameter or code path that could render any of the excluded
 * fields, not just "chooses not to show" them.
 *
 * Code Cleanup pass: the previous disclosed simplification ("shows all
 * salons") is resolved — [ai.rojan.designlab.data.demo.DemoService]
 * gained real multi-salon support
 * ([ai.rojan.designlab.data.demo.DemoService.offeredBySalonIds]), so
 * [CatalogEngine.salonsOfferingAllServices] now does a genuine
 * intersection: only salons that offer EVERY selected service appear.
 * "Do not show unrelated salons" is enforced by the filter itself, not
 * a display choice on top of an unfiltered list.
 */
@Composable
fun SalonListScreen(
    selectedServiceIds: List<String>,
    onBackClick: () -> Unit,
    onSalonSelected: (String) -> Unit,
) {
    val catalogEngine = remember { CatalogEngine() }
    val matchingSalons = remember(selectedServiceIds) {
        catalogEngine.salonsOfferingAllServices(selectedServiceIds)
    }

    PremiumBackground {
        Column(modifier = Modifier.fillMaxSize().padding(RojanDimens.SpaceMD)) {
            GlassBackButton(onClick = onBackClick)

            Text(
                text = "انتخاب سالن",
                style = RojanTypography.HeroTitle,
                color = RojanTextOnGlass,
                modifier = Modifier.padding(vertical = RojanDimens.SpaceMD),
            )

            if (matchingSalons.isEmpty()) {
                Text(
                    text = "سالنی با تمام خدمات انتخابی یافت نشد",
                    style = RojanTypography.Body,
                    color = RojanLuxurySecondaryBody,
                )
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                items(matchingSalons) { salon ->
                    MinimalSalonCard(salon = salon, onClick = { onSalonSelected(salon.id) })
                }
            }
        }
    }
}

@Composable
private fun MinimalSalonCard(salon: DemoSalon, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
        shape = RojanShapes.Small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(salon.colorSeed.copy(alpha = 0.35f), RojanShapes.Small),
                contentAlignment = Alignment.Center,
            ) {
                if (salon.assetRes != null) {
                    RojanSampleImage(
                        resId = salon.assetRes,
                        contentDescription = salon.name,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(Icons.Filled.Storefront, contentDescription = null, tint = RojanTextPrimary)
                }
            }

            Column {
                Text(
                    salon.name,
                    style = RojanTypography.Body.rojanPressedShadow(interactionSource),
                    color = RojanTextPrimary,
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS)) {
                    Icon(Icons.Filled.Star, contentDescription = "امتیاز", tint = RojanRatingGold, modifier = Modifier.size(RojanDimens.IconSizeSmall))
                    Text(salon.rating, style = RojanTypography.Caption, color = RojanTextSecondary)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS)) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = RojanTextSecondary, modifier = Modifier.size(RojanDimens.IconSizeSmall))
                    Text("${salon.distanceKm} km", style = RojanTypography.Caption, color = RojanTextSecondary)
                }
            }
        }
    }
}
