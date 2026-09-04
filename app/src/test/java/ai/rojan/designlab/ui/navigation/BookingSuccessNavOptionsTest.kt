package ai.rojan.designlab.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sprint 5B-7 (5B7-2) — the [NavOptions] `navigateHomeAfterBooking` applies.
 * A hosted-graph back-stack test lives in the instrumentation suite; this
 * pins the mechanism (clear-whole-graph + single-top) with no device.
 */
class BookingSuccessNavOptionsTest {

    @Test
    fun `booking success clears the whole graph and is single-top`() {
        val graphId = 4242
        val options = bookingSuccessNavOptions(graphId)

        assertEquals("pops up to the graph root", graphId, options.popUpToId)
        assertTrue("inclusive — the graph's own start entry is cleared too", options.isPopUpToInclusive())
        assertTrue("single-top — no duplicate home is pushed", options.shouldLaunchSingleTop())
        assertEquals("no shallow restore of the cleared destinations", false, options.shouldRestoreState())
    }
}
