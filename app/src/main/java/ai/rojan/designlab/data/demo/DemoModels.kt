package ai.rojan.designlab.data.demo

import androidx.compose.ui.graphics.Color

/**
 * Demo data models — Production Demo Completion (Journey 1).
 *
 * Plain data classes, no persistence, no networking, no Clean
 * Architecture domain/data layering — deliberately lightweight, per this
 * phase's explicit "DO NOT connect Backend / DO NOT add networking"
 * scope. [colorSeed] stands in for a real photo asset (per "use only
 * existing assets, keep placeholder if none exists") — the same tinted
 * illustration-placeholder approach already used throughout Customer
 * Home and Booking, not a new visual concept.
 */
data class DemoSalon(
    val id: String,
    val name: String,
    val tagline: String,
    val rating: String,
    val reviewCount: Int,
    val address: String,
    val distanceKm: String,
    val workingHours: String,
    val phone: String,
    val facilities: List<String>,
    val colorSeed: Color,
    /** Architecture Cleanup Sprint (Task 3): absorbed from FeaturedSalons/NearbySalons' previous separate local data classes - real asset image when available, null falls back to an icon placeholder in the UI. */
    val assetRes: Int? = null,
)

data class DemoSpecialist(
    val id: String,
    val name: String,
    val title: String,
    val bio: String,
    val experienceYears: Int,
    val rating: String,
    val reviewCount: Int,
    val completedAppointments: Int,
    val skills: List<String>,
    val languages: List<String>,
    val salonId: String,
    val colorSeed: Color,
    /** Architecture Cleanup Sprint (Task 3): absorbed from TopSpecialists' previous separate local data class. */
    val assetRes: Int? = null,
)

data class DemoService(
    val id: String,
    val name: String,
    val description: String,
    val durationMinutes: Int,
    val price: Int,
    val discountPrice: Int?,
    val benefits: List<String>,
    val preparation: String,
    val aftercare: String,
    val salonId: String,
    val colorSeed: Color,
    /** Booking Experience Refactor: which of DemoServiceCategoryRepository's 10 categories this belongs to - needed for the category-first flow (spec section 8). */
    val categoryLabel: String = "",
    /** Booking Experience Refactor, Salon List filtering: other salons that ALSO genuinely offer this same service - [salonId] stays the "primary" salon (unchanged for every existing single-salon call site), this is additive. [offeredBySalonIds] (below) is the real, complete list to filter against. */
    val additionalSalonIds: List<String> = emptyList(),
) {
    /** Every salon that genuinely offers this service - the real basis for "salons capable of providing this service," not just [salonId] alone. */
    val offeredBySalonIds: List<String> get() = listOf(salonId) + additionalSalonIds
}

/**
 * Booking Engine completion: [startMinutes] (minutes since midnight)
 * added so [ai.rojan.designlab.domain.booking.rules.BookingAvailabilityRules]
 * can do real interval arithmetic (does a contiguous run of slots cover
 * a service's full duration?) instead of only checking one slot at a
 * time. [time] stays as the display string; [startMinutes] is the
 * machine-usable form derived from it.
 */
data class DemoTimeSlot(
    val time: String,
    val startMinutes: Int,
    val available: Boolean,
)

data class DemoReview(
    val authorName: String,
    val rating: String,
    val comment: String,
    val daysAgo: Int,
)
