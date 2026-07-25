package ai.rojan.designlab.screens.specialist

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.ui.background.PremiumBackground
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

/** Journey 1, Screen 3: Specialist Profile. */
@Composable
fun SpecialistProfileScreen(
    specialistId: String,
    onBackClick: () -> Unit,
    onServiceClick: (String) -> Unit,
) {
    val catalogEngine = remember { CatalogEngine() }
    val specialist = catalogEngine.findSpecialistById(specialistId)

    PremiumBackground {
        if (specialist == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("متخصص یافت نشد", color = RojanTextOnGlass, style = RojanTypography.Body)
            }
            return@PremiumBackground
        }

        val services = catalogEngine.servicesForSalon(specialist.salonId)
        val reviews = catalogEngine.reviewsFor(specialistId)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item { GlassBackButton(onClick = onBackClick) }

            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .background(specialist.colorSeed.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Person, null, tint = RojanTextOnGlass, modifier = Modifier.size(48.dp))
                    }
                    Spacer(modifier = Modifier.height(RojanDimens.SpaceSM))
                    Text(specialist.name, style = RojanTypography.HeroTitle, color = RojanTextOnGlass)
                    Text(specialist.title, style = RojanTypography.Body, color = RojanTextOnDarkSurface)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatChip("${specialist.experienceYears}", "سال تجربه")
                    StatChip(specialist.rating, "امتیاز")
                    StatChip("${specialist.completedAppointments}", "نوبت انجام‌شده")
                }
            }

            item {
                GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
                    Text(
                        text = specialist.bio,
                        style = RojanTypography.Body,
                        color = RojanTextPrimary,
                        modifier = Modifier.padding(RojanDimens.SpaceMD),
                    )
                }
            }

            item { Text("مهارت‌ها", style = RojanTypography.Body, color = RojanTextOnGlass) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                    specialist.skills.forEach { skill ->
                        GlassSurface(shape = RojanShapes.Small) {
                            Row(
                                modifier = Modifier.padding(horizontal = RojanDimens.SpaceSM, vertical = RojanDimens.SpaceXS),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.CheckCircle, null, tint = RojanAIGlow, modifier = Modifier.size(RojanDimens.IconSizeSmall))
                                Text(" $skill", style = RojanTypography.Caption, color = RojanTextPrimary)
                            }
                        }
                    }
                }
            }

            if (services.isNotEmpty()) {
                item { Text("خدمات قابل رزرو", style = RojanTypography.Body, color = RojanTextOnGlass) }
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
                            Text(service.name, style = RojanTypography.Body, color = RojanTextPrimary)
                            Text("${service.durationMinutes} دقیقه", style = RojanTypography.Caption, color = RojanTextSecondary)
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
                                Icon(Icons.Filled.Star, null, tint = RojanRatingGold, modifier = Modifier.size(RojanDimens.IconSizeSmall))
                                Text(" ${review.rating}", style = RojanTypography.Caption, color = RojanTextSecondary)
                            }
                        }
                        Text(review.comment, style = RojanTypography.Caption, color = RojanTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = RojanTypography.HeroTitle, color = RojanAIGlow)
        Text(label, style = RojanTypography.Caption, color = RojanTextOnDarkSurface)
    }
}
