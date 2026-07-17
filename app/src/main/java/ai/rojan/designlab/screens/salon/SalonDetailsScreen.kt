package ai.rojan.designlab.screens.salon

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.ui.background.PremiumBackground
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanRatingGold
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnDarkSurface
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.components.icon.RojanIconContainer

/**
 * Journey 1, Screen 2: Salon Details.
 *
 * Booking Experience Refactor, spec section 10: when reached from the
 * new category-first flow, [selectedServiceIds] is non-null and
 * [services] is filtered to only those — "Only the services already
 * selected by the user. Do NOT display every service offered by the
 * salon." When `null` (the original Journey 1 browse-first entry
 * point), every salon service still shows, unchanged.
 *
 * [onContinueBooking], when provided, renders a bottom CTA implementing
 * "If only one specialist exists: Skip specialist selection completely"
 * — approximated here as "only one specialist at this salon" (spec asks
 * for "capable of performing the selected services" specifically, but
 * no capability-to-service mapping exists in the data model yet; this
 * is a disclosed simplification, not silently different behavior).
 *
 * If [salonId] doesn't resolve to a real demo entry (shouldn't happen
 * via normal navigation, but defensive nonetheless), shows a simple
 * "not found" state rather than crashing — no dead-end, per this
 * phase's "no empty pages" rule applied defensively.
 */
@Composable
fun SalonDetailsScreen(
    salonId: String,
    onBackClick: () -> Unit,
    onSpecialistClick: (String) -> Unit,
    onServiceClick: (String) -> Unit,
    selectedServiceIds: List<String>? = null,
    onContinueBooking: ((autoSelectedSpecialistId: String?) -> Unit)? = null,
) {
    val catalogEngine = remember { CatalogEngine() }
    val salon = catalogEngine.findSalonById(salonId)

    PremiumBackground {
        if (salon == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("سالن یافت نشد", color = RojanTextOnGlass, style = RojanTypography.Body)
            }
            return@PremiumBackground
        }

        val specialists = catalogEngine.specialistsForSalon(salonId)
        val allServices = catalogEngine.servicesForSalon(salonId)
        val services = if (selectedServiceIds != null) {
            allServices.filter { it.id in selectedServiceIds }
        } else {
            allServices
        }
        val reviews = catalogEngine.reviewsFor(salonId)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassBackButton(onClick = onBackClick)
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(salon.colorSeed.copy(alpha = 0.5f), RojanShapes.GlassCard),
                    contentAlignment = Alignment.Center,
                ) {
                    RojanIconContainer(
    imageVector = Icons.Filled.Storefront,
    contentDescription = null,
    tint = RojanTextOnGlass,
    sizeOverride = 56.dp,
)
                }
            }

            item {
                Column {
                    Text(salon.name, style = RojanTypography.HeroTitle, color = RojanTextOnGlass)
                    Text(salon.tagline, style = RojanTypography.Body, color = RojanTextOnDarkSurface)
                }
            }

            item {
                GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RojanDimens.SpaceMD),
                        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                    ) {
                        InfoRow(Icons.Filled.Star, "${salon.rating} (${salon.reviewCount} نظر)")
                        InfoRow(Icons.Filled.LocationOn, salon.address)
                        InfoRow(Icons.Filled.AccessTime, salon.workingHours)
                        InfoRow(Icons.Filled.Phone, salon.phone)
                    }
                }
            }

            item {
                Text("امکانات", style = RojanTypography.Body, color = RojanTextOnGlass)
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                ) {
                    salon.facilities.forEach { facility ->
                        GlassSurface(shape = RojanShapes.Small) {
                            Text(
                                text = facility,
                                style = RojanTypography.Caption,
                                color = RojanTextPrimary,
                                modifier = Modifier.padding(horizontal = RojanDimens.SpaceSM, vertical = RojanDimens.SpaceXS),
                            )
                        }
                    }
                }
            }

            if (specialists.isNotEmpty()) {
                item { Text("متخصصان", style = RojanTypography.Body, color = RojanTextOnGlass) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                        items(specialists) { specialist ->
                            GlassSurface(
                                modifier = Modifier
                                    .clickable { onSpecialistClick(specialist.id) },
                                shape = RojanShapes.Small,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(RojanDimens.SpaceSM)
                                        .width(120.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(specialist.colorSeed.copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.Person, null, tint = RojanTextPrimary)
                                    }
                                    Spacer(modifier = Modifier.height(RojanDimens.SpaceXS))
                                    Text(
                                        specialist.name,
                                        style = RojanTypography.Caption,
                                        color = RojanTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (services.isNotEmpty()) {
                item { Text("خدمات", style = RojanTypography.Body, color = RojanTextOnGlass) }
                items(services) { service ->
                    GlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onServiceClick(service.id) },
                        shape = RojanShapes.Small,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(RojanDimens.SpaceMD),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(service.name, style = RojanTypography.Body, color = RojanTextPrimary)
                                Text("${service.durationMinutes} دقیقه", style = RojanTypography.Caption, color = RojanTextSecondary)
                            }
                            Text(
                                text = "${(service.discountPrice ?: service.price).toString()} تومان",
                                style = RojanTypography.Body,
                                color = RojanAIGlow,
                            )
                        }
                    }
                }
            }

            item { Text("نظرات", style = RojanTypography.Body, color = RojanTextOnGlass) }
            items(reviews) { review ->
                GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
                    Column(modifier = Modifier.padding(RojanDimens.SpaceMD)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(review.authorName, style = RojanTypography.Caption, color = RojanTextPrimary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, null, tint = RojanRatingGold, modifier = Modifier.size(14.dp))
                                Text(" ${review.rating}", style = RojanTypography.Caption, color = RojanTextSecondary)
                            }
                        }
                        Text(review.comment, style = RojanTypography.Caption, color = RojanTextSecondary)
                    }
                }
            }

            if (onContinueBooking != null) {
                item {
                    PremiumButton(
                        text = "ادامه رزرو",
                        onClick = {
                            val autoSpecialistId = if (specialists.size == 1) specialists.first().id else null
                            onContinueBooking(autoSpecialistId)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = RojanTextSecondary, modifier = Modifier.size(18.dp))
        Text(" $text", style = RojanTypography.Caption, color = RojanTextSecondary)
    }
}
