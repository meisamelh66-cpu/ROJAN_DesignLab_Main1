package ai.rojan.designlab.presentation.booking

import ai.rojan.designlab.domain.booking.BookingIntent
import ai.rojan.designlab.domain.booking.BookingState
import ai.rojan.designlab.domain.booking.BookingStep
import ai.rojan.designlab.domain.booking.PaymentMethod
import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sprint 5B-8B — the customer [BookingViewModel] is the reference
 * `SavedStateHandle` implementation the Manager/Reception booking VMs were
 * ported from (5B-6), and the only one with no unit test. It holds the
 * whole customer booking session; process death mid-flow must return the
 * customer to the same step with the same selections (its own doc records
 * this was `am kill`-reproduced).
 *
 * `BookingViewModel` is fully synchronous (`dispatch` → `reducer.applyAll`
 * → `persistState`), so no dispatcher / `runTest` is needed.
 */
class BookingViewModelSavedStateTest {

    private fun vm(handle: SavedStateHandle = SavedStateHandle()) = BookingViewModel(savedStateHandle = handle)

    // ---- restoration -------------------------------------------

    @Test
    fun `every selection is restored from the same SavedStateHandle`() {
        val handle = SavedStateHandle()

        val first = vm(handle)
        first.onIntentDetected(BookingIntent.SEARCH)
        first.onSalonSelected("salon-1")
        first.onSpecialistSelected("spec-1")
        first.onServiceSelected("svc-1")
        first.onPackageSelected("pkg-1")
        first.onDateSelected("2026/09/01")
        first.onTimeSelected("10:00")
        first.onPromotionApplied("promo-1")
        first.onCouponApplied("coupon-1")
        first.onPaymentMethodSelected(PaymentMethod.PAY_AT_SALON)

        // simulate process death: a brand-new instance from the restored handle
        val restored = vm(handle).state

        assertEquals(BookingIntent.SEARCH, restored.intent)
        assertEquals("salon-1", restored.salonId)
        assertEquals("spec-1", restored.specialistId)
        assertEquals("svc-1", restored.serviceId)
        assertEquals("pkg-1", restored.packageId)
        assertEquals("2026/09/01", restored.selectedDateKey)
        assertEquals("10:00", restored.selectedTime)
        assertEquals("promo-1", restored.promotionId)
        assertEquals("coupon-1", restored.couponId)
        assertEquals(PaymentMethod.PAY_AT_SALON, restored.paymentMethod)
    }

    @Test
    fun `a fresh SavedStateHandle yields a default empty session`() {
        val s = vm().state
        assertEquals(BookingState(), s)
        assertEquals(BookingIntent.UNKNOWN, s.intent)
        assertEquals(PaymentMethod.WALLET, s.paymentMethod)
        assertNull(s.salonId)
        assertNull(s.serviceId)
    }

    // ---- step resolution after restore ------------------------

    @Test
    fun `a fully populated restored session resumes at CONFIRMATION`() {
        val handle = SavedStateHandle()
        vm(handle).apply {
            onServiceSelected("svc-1")
            onSpecialistSelected("spec-1")
            onDateSelected("2026/09/01")
            onTimeSelected("10:00")
        }

        val restored = vm(handle)
        assertEquals(BookingStep.CONFIRMATION, restored.nextStep())
        assertTrue(restored.isReadyForConfirmation())
    }

    @Test
    fun `a partially populated restored session resumes at the first missing step`() {
        val handle = SavedStateHandle()
        vm(handle).apply {
            onServiceSelected("svc-1")
            // no specialist / date / time
        }

        val restored = vm(handle)
        assertEquals(BookingStep.SPECIALIST, restored.nextStep())
        assertFalse(restored.isReadyForConfirmation())
    }

    @Test
    fun `a restored session with no service resumes at SEARCH`() {
        val handle = SavedStateHandle()
        vm(handle).onSalonSelected("salon-1") // salon alone is not a gate

        assertEquals(BookingStep.SEARCH, vm(handle).nextStep())
    }

    // ---- reducer / completion rules -------------------------

    @Test
    fun `picking a new date clears the previously chosen time in the persisted state`() {
        val handle = SavedStateHandle()
        vm(handle).apply {
            onDateSelected("2026/09/01")
            onTimeSelected("10:00")
            onDateSelected("2026/09/02")
        }

        val restored = vm(handle).state
        assertEquals("2026/09/02", restored.selectedDateKey)
        assertNull("a new date invalidates the old slot", restored.selectedTime)
    }

    @Test
    fun `a restored time with no date is reconciled away by step resolution`() {
        // stale partial: time set, date not — BookingStateCompletion nulls the time
        val handle = SavedStateHandle(
            mapOf(
                "booking_service_id" to "svc-1",
                "booking_specialist_id" to "spec-1",
                "booking_time" to "10:00",
                // no "booking_date_key"
            ),
        )
        val restored = vm(handle)

        assertEquals(BookingStep.DATE, restored.nextStep())
        assertFalse(restored.isReadyForConfirmation())
    }

    // ---- intent origin -------------------------------------

    @Test
    fun `intent records how the session started and a later action never overwrites it`() {
        val handle = SavedStateHandle()
        val vm = vm(handle)

        vm.onIntentDetected(BookingIntent.FAVORITE)
        assertEquals(BookingIntent.FAVORITE, vm.state.intent)

        vm.onIntentDetected(BookingIntent.SEARCH) // a different origin arriving later
        assertEquals("origin is immutable once set", BookingIntent.FAVORITE, vm.state.intent)
        assertEquals(BookingIntent.FAVORITE, vm(handle).state.intent) // and it persists
    }

    // ---- corrupt / drifted persisted values ---------------

    @Test
    fun `an unparseable persisted intent or payment method falls back to the default, not a crash`() {
        // schema drift / a corrupted bundle — restoreState()'s runCatching guards this
        val handle = SavedStateHandle(
            mapOf(
                "booking_intent" to "SOME_REMOVED_INTENT",
                "booking_payment_method" to "GARBAGE",
                "booking_salon_id" to "salon-1",
            ),
        )
        val restored = vm(handle).state

        assertEquals(BookingIntent.UNKNOWN, restored.intent)
        assertEquals(PaymentMethod.WALLET, restored.paymentMethod)
        assertEquals("valid fields still restore", "salon-1", restored.salonId)
    }
}
