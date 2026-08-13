package ai.rojan.designlab.reception.domain.booking

import ai.rojan.designlab.domain.repository.Service
import ai.rojan.designlab.domain.repository.Specialist
import ai.rojan.designlab.reception.domain.repository.ReceptionCustomer

/**
 * The in-progress state for the Reception booking wizard (Start → Customer
 * → Service → Specialist → DateTime → Review → Success) — same role as
 * [ai.rojan.designlab.manager.domain.booking.ManagerBookingState], kept as
 * Reception's own type per the established "each flavor owns its own
 * domain types" precedent (`ReceptionAuthState.kt`'s own doc comment).
 *
 * [time] must only ever be set from a raw ISO-8601 `start` value returned
 * by a real [ai.rojan.designlab.domain.repository.AvailabilityRepository.getAvailableSlots]
 * result — never reconstructed client-side — so [ReceptionBookingViewModel.confirm]
 * only ever submits a slot the backend itself already confirmed was free
 * at query time.
 */
data class ReceptionBookingState(
    val customer: ReceptionCustomer? = null,
    val service: Service? = null,
    val specialist: Specialist? = null,
    val dateIso: String? = null,
    val time: String? = null,
    val isSubmitting: Boolean = false,
    val confirmError: String? = null,
    val createdBookingId: String? = null,
)
