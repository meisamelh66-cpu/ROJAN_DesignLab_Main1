package ai.rojan.designlab.reception.data

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.repository.AvailabilityRepository
import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.SpecialistRepository
import ai.rojan.designlab.reception.domain.repository.ReceptionBookingRepository
import ai.rojan.designlab.reception.domain.repository.ReceptionCustomerRepository
import android.content.Context

/**
 * Per-salon repository bundle for the Reception app — the counterpart to
 * [ai.rojan.designlab.manager.data.ManagerRepositories], deliberately
 * simpler: [salonId] is supplied directly by the caller, already resolved
 * from a real [ai.rojan.designlab.reception.domain.auth.ActiveSalonUiState.Active]
 * (itself sourced from `GET /users/me/salon-access`) by the time any
 * screen that needs this bundle is ever reachable — Reception's nav graph
 * gates every such screen behind that resolution already (see
 * `ReceptionRootGraph.kt`/`ReceptionNavGraph.kt`). This is why, unlike
 * `ManagerRepositories`, there is no `initialize()`, no nullable/`Empty*`
 * fallback objects, and no re-derivation via the owner-only
 * `GET /salons/mine` — the exact gap `ROJAN_Reception_Phase1_Readiness_Report_v1.md`
 * §3 flagged in `ManagerRepositories.initialize()` (it resolves salon
 * identity via ownership, which a receptionist's membership-based access
 * would never satisfy). Construction here does no I/O and cannot fail.
 *
 * [bookingRepository]/[customerRepository] are real, backend-backed
 * (reusing the existing [ai.rojan.designlab.data.remote.ManagerBookingApi]/
 * [ai.rojan.designlab.data.remote.ManagerCustomerApi] Retrofit interfaces,
 * not duplicating them) — calls through them will legitimately fail
 * authorization until `ROJAN_System1_Backend_Decision_v2.md` §4 item 6
 * ships; that is the correct behavior, not something this class works
 * around. [serviceRepository]/[specialistRepository]/[availabilityRepository]/
 * [genericBookingRepository] are the existing, already-real, already-
 * "any authenticated user" top-level repositories — reused directly, not
 * reimplemented, since nothing about them is Manager-specific.
 */
class ReceptionRepositories(
    val salonId: String,
    val bookingRepository: ReceptionBookingRepository,
    val customerRepository: ReceptionCustomerRepository,
    val serviceRepository: ServiceRepository,
    val serviceCategoryRepository: ServiceCategoryRepository,
    val specialistRepository: SpecialistRepository,
    val availabilityRepository: AvailabilityRepository,
    /** Booking-id-scoped operations (confirm/complete/cancel/reschedule) — flavor-agnostic, reused as-is. */
    val genericBookingRepository: BookingRepository,
) {
    companion object {
        fun from(context: Context, salonId: String): ReceptionRepositories {
            val container = BackendApiContainerHolder.get(context)
            return ReceptionRepositories(
                salonId = salonId,
                bookingRepository = BackendReceptionBookingRepository(container.managerBookingApi),
                customerRepository = BackendReceptionCustomerRepository(container.managerCustomerApi),
                serviceRepository = container.serviceRepository,
                serviceCategoryRepository = container.serviceCategoryRepository,
                specialistRepository = container.specialistRepository,
                availabilityRepository = container.availabilityRepository,
                genericBookingRepository = container.bookingRepository,
            )
        }
    }
}
