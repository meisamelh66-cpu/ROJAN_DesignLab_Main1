package ai.rojan.designlab.navigation

import ai.rojan.designlab.domain.booking.BookingStep
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Sprint 5B-8D — [RojanDestinations.routeForBookingStep] is the single
 * pure-Kotlin seam between the Navigation-free booking domain
 * ([BookingStep]) and real route strings. Every post-selection hop in
 * `RojanNavGraph` routes through it, but it had no direct test — a
 * reorder or typo in its `when` silently misroutes the whole flow.
 *
 * The `SPECIALIST` + null-`salonId` branch is the "invalid booking state"
 * fallback (STEP 2): it must degrade to SEARCH, not crash or build a
 * malformed `specialist_selection/null` route.
 */
class RojanBookingStepRouteTest {

    @Test
    fun `SPECIALIST with a salon id builds the parametrized specialist-selection route`() {
        assertEquals(
            RojanDestinations.specialistSelection("salon-42"),
            RojanDestinations.routeForBookingStep(BookingStep.SPECIALIST, salonId = "salon-42"),
        )
    }

    @Test
    fun `SPECIALIST with no salon id falls back to SEARCH rather than a malformed route`() {
        assertEquals(
            RojanDestinations.SEARCH,
            RojanDestinations.routeForBookingStep(BookingStep.SPECIALIST, salonId = null),
        )
    }

    @Test
    fun `SEARCH, SALON and SERVICE steps all map to the SEARCH entry point`() {
        listOf(BookingStep.SEARCH, BookingStep.SALON, BookingStep.SERVICE).forEach { step ->
            assertEquals("$step should route to SEARCH", RojanDestinations.SEARCH, route(step))
        }
    }

    @Test
    fun `DATE, TIME, CONFIRMATION and SUCCESS map to their own routes`() {
        assertEquals(RojanDestinations.BOOKING_DATE, route(BookingStep.DATE))
        assertEquals(RojanDestinations.BOOKING_TIME, route(BookingStep.TIME))
        assertEquals(RojanDestinations.BOOKING_CONFIRMATION, route(BookingStep.CONFIRMATION))
        assertEquals(RojanDestinations.BOOKING_SUCCESS, route(BookingStep.SUCCESS))
    }

    @Test
    fun `every BookingStep resolves to a non-blank route for both salon-id states`() {
        BookingStep.entries.forEach { step ->
            listOf("salon-1", null).forEach { salonId ->
                val r = RojanDestinations.routeForBookingStep(step, salonId)
                assert(r.isNotBlank()) { "$step / salonId=$salonId produced a blank route" }
            }
        }
    }

    private fun route(step: BookingStep) = RojanDestinations.routeForBookingStep(step, salonId = "salon-1")
}
