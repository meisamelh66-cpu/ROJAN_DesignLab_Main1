package ai.rojan.designlab.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.data.demo.DemoSpecialist
import ai.rojan.designlab.domain.booking.BookingEngine
import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.ui.background.WarmBackground
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.image.SpecialistAvatar
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanRatingGold
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Booking Experience Refactor, spec section 11 — Specialist Selection.
 * "Priority order: 1. Earliest available appointment, 2. Highest
 * rating, 3. Salon priority."
 *
 * Booking Engine completion: criterion 1 is now real, not a disclosed
 * gap. For each specialist, this walks [CatalogEngine.availableDates]
 * in order and finds their first date with any open slot for
 * [durationMinutes] via [BookingEngine.hasAnyAvailability], then that
 * date's actual earliest slot via [BookingEngine.earliestAvailableSlot].
 * Specialists are sorted by (day index, slot start time) ascending —
 * genuinely earliest-available first. Rating (criterion 2) is the
 * tiebreaker when two specialists' earliest slots land on the exact
 * same day+time. "Salon priority" (criterion 3) isn't modeled - this
 * screen is already scoped to one salon (no cross-salon ranking
 * happens here), so it doesn't apply within this screen's boundary.
 *
 * The "skip this page" half of the parent rule is enforced one level
 * up, in Navigation (this screen is simply never navigated to when
 * [ai.rojan.designlab.screens.salon.SalonDetailsScreen] already found
 * exactly one specialist).
 */
@Composable
fun SpecialistSelectionScreen(
    salonId: String,
    durationMinutes: Int,
    onBackClick: () -> Unit,
    onSpecialistSelected: (String) -> Unit,
) {
    val catalogEngine = remember { CatalogEngine() }
    val bookingEngine = remember { BookingEngine() }
    val dates = remember { catalogEngine.availableDates() }

    val specialists = remember(salonId, durationMinutes) {
        catalogEngine.specialistsForSalon(salonId)
            .map { specialist ->
                val earliestDate = dates.firstOrNull { (key, _) ->
                    bookingEngine.hasAnyAvailability(key, durationMinutes, specialist.id)
                }
                val earliestSlotMinutes = earliestDate?.let { (key, _) ->
                    bookingEngine.earliestAvailableSlot(key, durationMinutes, specialist.id)?.startMinutes
                }
                val dayIndex = earliestDate?.let { dates.indexOf(it) } ?: Int.MAX_VALUE
                Triple(specialist, dayIndex, earliestSlotMinutes ?: Int.MAX_VALUE)
            }
            .sortedWith(compareBy({ it.second }, { it.third }, { -it.first.rating.toFloatOrNull().let { r -> r ?: 0f } }))
            .map { it.first }
    }

    WarmBackground {
        Column(modifier = Modifier.fillMaxSize().padding(RojanDimens.SpaceMD)) {
            GlassBackButton(onClick = onBackClick)

            Text(
                text = "انتخاب متخصص",
                style = RojanTypography.HeroTitle,
                color = RojanTextOnGlass,
                modifier = Modifier.padding(vertical = RojanDimens.SpaceMD),
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                items(specialists) { specialist ->
                    SpecialistRow(specialist = specialist, onClick = { onSpecialistSelected(specialist.id) })
                }
            }
        }
    }
}

@Composable
private fun SpecialistRow(specialist: DemoSpecialist, onClick: () -> Unit) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RojanShapes.Small,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            Box(
                modifier = Modifier.size(56.dp).background(specialist.colorSeed.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                SpecialistAvatar(
                    assetRes = specialist.assetRes,
                    contentDescription = specialist.name,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column {
                Text(specialist.name, style = RojanTypography.Body, color = RojanTextPrimary)
                Text(specialist.title, style = RojanTypography.Caption, color = RojanTextSecondary)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS)) {
                    Icon(Icons.Filled.Star, contentDescription = "امتیاز", tint = RojanRatingGold, modifier = Modifier.size(RojanDimens.IconSizeSmall))
                    Text(specialist.rating, style = RojanTypography.Caption, color = RojanTextSecondary)
                }
            }
        }
    }
}
