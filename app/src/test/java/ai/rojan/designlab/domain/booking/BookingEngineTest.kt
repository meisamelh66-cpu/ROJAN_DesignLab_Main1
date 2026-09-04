package ai.rojan.designlab.domain.booking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sprint 5B-8D — [BookingEngine] is the facade `BookingViewModel` and
 * `RojanNavGraph` ask "what's the next step / can we confirm / should we
 * record this intent". `BookingViewModelSavedStateTest` covers a few
 * paths through it via the VM; this pins the full contract directly:
 *
 *  - the step ladder walks SEARCH → SPECIALIST → DATE → TIME → CONFIRMATION,
 *    each step gated only by its own missing field (no fixed sequence);
 *  - `determineNextStep` runs `BookingStateCompletion` *before*
 *    `BookingStepResolver`, so a time with no date is reconciled away and
 *    the next step is DATE, never TIME (the seam not covered elsewhere);
 *  - `decideIntent` records an origin only while the session is UNKNOWN;
 *  - `isReadyForConfirmation` requires service + date + time together.
 */
class BookingEngineTest {

    private val engine = BookingEngine()

    private val complete = BookingState(
        serviceId = "svc-1",
        specialistId = "spec-1",
        selectedDateKey = "2026-09-10",
        selectedTime = "10:00",
    )

    // ---- determineNextStep: the step ladder ----------------------

    @Test
    fun `an empty session asks for the service first`() {
        assertEquals(BookingStep.SEARCH, engine.determineNextStep(BookingState()))
    }

    @Test
    fun `salon alone is not a gate - still asks for the service`() {
        assertEquals(BookingStep.SEARCH, engine.determineNextStep(BookingState(salonId = "salon-1")))
    }

    @Test
    fun `with a service but no specialist it asks for the specialist`() {
        assertEquals(BookingStep.SPECIALIST, engine.determineNextStep(BookingState(serviceId = "svc-1")))
    }

    @Test
    fun `with service and specialist it asks for the date`() {
        assertEquals(
            BookingStep.DATE,
            engine.determineNextStep(BookingState(serviceId = "svc-1", specialistId = "spec-1")),
        )
    }

    @Test
    fun `with service, specialist and date it asks for the time`() {
        assertEquals(
            BookingStep.TIME,
            engine.determineNextStep(
                BookingState(serviceId = "svc-1", specialistId = "spec-1", selectedDateKey = "2026-09-10"),
            ),
        )
    }

    @Test
    fun `a fully populated session resolves to CONFIRMATION`() {
        assertEquals(BookingStep.CONFIRMATION, engine.determineNextStep(complete))
    }

    // ---- determineNextStep: completion runs before resolution ----

    @Test
    fun `a time with no date is reconciled away - next step is DATE, never TIME`() {
        val stale = BookingState(serviceId = "svc-1", specialistId = "spec-1", selectedTime = "10:00")
        assertEquals(BookingStep.DATE, engine.determineNextStep(stale))
    }

    // ---- decideIntent -------------------------------------------

    @Test
    fun `decideIntent records the origin while the session intent is UNKNOWN`() {
        val events = engine.decideIntent(BookingState(), BookingIntent.FAVORITE)
        assertEquals(listOf(BookingEvent.IntentDetected(BookingIntent.FAVORITE)), events)
    }

    @Test
    fun `decideIntent emits nothing once an origin is already recorded`() {
        val events = engine.decideIntent(BookingState(intent = BookingIntent.SEARCH), BookingIntent.FAVORITE)
        assertTrue(events.isEmpty())
    }

    // ---- isReadyForConfirmation --------------------------------

    @Test
    fun `isReadyForConfirmation is true only when service, date and time are all set`() {
        assertTrue(engine.isReadyForConfirmation(complete))
    }

    @Test
    fun `isReadyForConfirmation is false when any one required field is missing`() {
        assertFalse("no service", engine.isReadyForConfirmation(complete.copy(serviceId = null)))
        assertFalse("no date", engine.isReadyForConfirmation(complete.copy(selectedDateKey = null)))
        assertFalse("no time", engine.isReadyForConfirmation(complete.copy(selectedTime = null)))
    }

    @Test
    fun `isReadyForConfirmation does not require a specialist`() {
        assertTrue(engine.isReadyForConfirmation(complete.copy(specialistId = null)))
    }

    @Test
    fun `isReadyForConfirmation is false for a stale time-without-date even though time is non-null`() {
        val stale = BookingState(serviceId = "svc-1", selectedTime = "10:00")
        assertFalse(engine.isReadyForConfirmation(stale))
    }
}
