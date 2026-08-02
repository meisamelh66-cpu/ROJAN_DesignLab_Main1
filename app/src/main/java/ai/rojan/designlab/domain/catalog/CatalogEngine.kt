package ai.rojan.designlab.domain.catalog

import ai.rojan.designlab.data.demo.DemoPromotion
import ai.rojan.designlab.data.demo.DemoPromotionRepository
import ai.rojan.designlab.data.demo.DemoReview
import ai.rojan.designlab.data.demo.DemoReviewRepository
import ai.rojan.designlab.data.demo.DemoSalon
import ai.rojan.designlab.data.demo.DemoSalonRepository
import ai.rojan.designlab.data.demo.DemoService
import ai.rojan.designlab.data.demo.DemoServiceCategory
import ai.rojan.designlab.data.demo.DemoServiceCategoryRepository
import ai.rojan.designlab.data.demo.DemoServiceRepository
import ai.rojan.designlab.data.demo.DemoSpecialist
import ai.rojan.designlab.data.demo.DemoSpecialistRepository
import ai.rojan.designlab.data.demo.DemoTimeSlotRepository

/**
 * Category 2 (Deterministic Computations) engine for the catalog/
 * discovery domain — Journey 1's Salon/Specialist/Service/TimeSlot/
 * Review data. Per the Enterprise dependency rule
 * (Infrastructure → Engine → ... → UI, "UI must never bypass the
 * Engine"), every screen that previously called a `Demo*Repository`
 * directly now calls this instead.
 *
 * No Rule Providers here — none of this is a business decision (find
 * by ID, filter by salon, search by name-contains are pure facts, not
 * judgment calls), and no Events/Reducer either — this reads static
 * catalog data, there's no session state being mutated. Same reasoning
 * as [ai.rojan.designlab.domain.customer.insights.ProfileInsightsEngine]
 * being read-only, applied to a different domain.
 *
 * **Booking Availability Architecture Correction:** `timeSlotsFor` used
 * to live here, but it embedded an actual availability *decision*
 * (which slots are bookable) — a Category 1 business decision, not a
 * Category 2 deterministic fact. Moved to
 * [ai.rojan.designlab.domain.booking.BookingEngine], which routes it
 * through [ai.rojan.designlab.domain.booking.rules.BookingAvailabilityRules]
 * instead. This class keeps only what's genuinely non-decisional —
 * [availableDates] and [dateLabelFor] are literal date labels, no
 * filtering or judgment involved.
 */
class CatalogEngine {

    fun findSalonById(salonId: String): DemoSalon? = DemoSalonRepository.findById(salonId)

    fun allSalons(): List<DemoSalon> = DemoSalonRepository.salons

    fun searchSalons(query: String): List<DemoSalon> =
        if (query.isBlank()) DemoSalonRepository.salons
        else DemoSalonRepository.salons.filter { it.name.contains(query) || it.tagline.contains(query) }

    fun findSpecialistById(specialistId: String): DemoSpecialist? = DemoSpecialistRepository.findById(specialistId)

    fun allSpecialists(): List<DemoSpecialist> = DemoSpecialistRepository.specialists

    fun specialistsForSalon(salonId: String): List<DemoSpecialist> = DemoSpecialistRepository.findBySalonId(salonId)

    fun findServiceById(serviceId: String): DemoService? = DemoServiceRepository.findById(serviceId)

    fun servicesForSalon(salonId: String): List<DemoService> = DemoServiceRepository.findBySalonId(salonId)

    fun allServices(): List<DemoService> = DemoServiceRepository.services

    fun servicesForCategory(categoryLabel: String): List<DemoService> = DemoServiceRepository.findByCategory(categoryLabel)

    /** Booking Experience Refactor, Salon List filtering: only salons that genuinely offer EVERY one of [serviceIds] - real intersection, not "shows everything." */
    fun salonsOfferingAllServices(serviceIds: List<String>): List<DemoSalon> {
        if (serviceIds.isEmpty()) return allSalons()
        val services = serviceIds.mapNotNull { DemoServiceRepository.findById(it) }
        if (services.size != serviceIds.size) return emptyList() // a selected service ID no longer resolves - defensive, shouldn't happen via normal navigation
        val salonIdSetsPerService = services.map { it.offeredBySalonIds.toSet() }
        val salonIdsOfferingAll = salonIdSetsPerService.reduce { acc, set -> acc.intersect(set) }
        return allSalons().filter { it.id in salonIdsOfferingAll }
    }

    fun serviceCategories(): List<DemoServiceCategory> = DemoServiceCategoryRepository.categories

    fun promotions(): List<DemoPromotion> = DemoPromotionRepository.promotions

    fun firstService(): DemoService? = DemoServiceRepository.services.firstOrNull()

    fun reviewsFor(entityId: String): List<DemoReview> = DemoReviewRepository.reviewsFor(entityId)

    fun availableDates(): List<Pair<String, String>> = DemoTimeSlotRepository.availableDates

    // Android <-> Backend Full Integration milestone: falls back to the raw
    // key instead of null when it's not one of the fixed demo keys — real
    // ISO dates from BookingDateScreen's now-real date selection don't
    // match this table (still demo/Phase-6-scoped) and previously produced
    // null, which silently blocked BookingConfirmationScreen's confirm
    // gate (`dateLabel != null`). This is a minimal regression fix, not a
    // Phase 6 rewrite of that screen.
    fun dateLabelFor(dateKey: String): String = DemoTimeSlotRepository.availableDates.find { it.first == dateKey }?.second ?: dateKey
}
