package ai.rojan.designlab.domain.booking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Sprint 5B-8D — [RollingBookingDates] feeds the booking-flow date picker
 * (ISO keys → the backend `available-slots` endpoint) and the
 * confirmation screen (`labelFor`). It had no test. The `labelFor`
 * fallback — an ISO key outside the current 7-day window returns the raw
 * key rather than an empty label — is the "stale / edited selection"
 * (invalid booking state) path from STEP 2.
 *
 * Assertions are kept clock-independent: shape, ordering, and the
 * offset-0/1 special-case labels, not specific calendar dates.
 */
class RollingBookingDatesTest {

    private val isoPattern = Regex("""\d{4}-\d{2}-\d{2}""")

    @Test
    fun `next7Days returns exactly seven entries`() {
        assertEquals(7, RollingBookingDates.next7Days().size)
    }

    @Test
    fun `every key is a well-formed ISO date`() {
        RollingBookingDates.next7Days().forEach { (iso, _) ->
            assertTrue("'$iso' is not yyyy-MM-dd", isoPattern.matches(iso))
        }
    }

    @Test
    fun `keys are seven consecutive days starting today, in ascending order`() {
        val dates = RollingBookingDates.next7Days().map { LocalDate.parse(it.first) }
        assertEquals(LocalDate.now(), dates.first())
        dates.zipWithNext().forEach { (a, b) ->
            assertEquals("consecutive days", a.plusDays(1), b)
        }
    }

    @Test
    fun `the first two labels are the today and tomorrow shortcuts`() {
        val labels = RollingBookingDates.next7Days().map { it.second }
        assertEquals("امروز", labels[0])
        assertEquals("فردا", labels[1])
    }

    @Test
    fun `labels from the third day on are non-blank weekday-and-month text`() {
        RollingBookingDates.next7Days().drop(2).forEach { (_, label) ->
            assertTrue("blank label", label.isNotBlank())
            assertTrue("'$label' missing the RTL name separator", label.contains("،"))
        }
    }

    @Test
    fun `labelFor round-trips every key inside the current window`() {
        RollingBookingDates.next7Days().forEach { (iso, label) ->
            assertEquals(label, RollingBookingDates.labelFor(iso))
        }
    }

    @Test
    fun `labelFor returns the raw key for a date outside the window`() {
        assertEquals("1999-01-01", RollingBookingDates.labelFor("1999-01-01"))
    }

    @Test
    fun `labelFor returns the raw input unchanged for an unparseable key`() {
        assertEquals("not-a-date", RollingBookingDates.labelFor("not-a-date"))
    }
}
