package ai.rojan.designlab.ui.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sprint 5B-7 (5B7-2) — [navigateHomeAfterBooking] must, after a completed
 * booking, leave exactly one home entry and no trace of the booking
 * sub-graph, whatever the pre-success back stack looked like.
 *
 * The helper is flavor-neutral, so one class covers Customer / Manager /
 * Reception and the Customer "log in while booking" path — each is just a
 * different starting stack shape.
 *
 * Instrumentation test — runs on a device/emulator when one is available.
 */
@RunWith(AndroidJUnit4::class)
class NavigateHomeAfterBookingTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun nav(startRoute: String): NavHostController {
        lateinit var controller: NavHostController
        composeRule.setContent {
            controller = rememberNavController()
            NavHost(navController = controller, startDestination = startRoute) {
                composable("explore") {}
                composable("home") {}
                composable("dashboard") {}
                composable("salonList") {}
                navigation(route = "bookingGraph", startDestination = "bookingStart") {
                    composable("bookingStart") {}
                    composable("bookingCustomer") {}
                    composable("bookingSuccess") {}
                }
            }
        }
        composeRule.waitForIdle()
        return controller
    }

    private fun push(controller: NavHostController, vararg routes: String) {
        composeRule.runOnUiThread { routes.forEach { controller.navigate(it) } }
        composeRule.waitForIdle()
    }

    /** Visible (ComposeNavigator) destination routes currently on the back stack. */
    private fun composeRoutes(controller: NavHostController): List<String> =
        controller.currentBackStack.value
            .filter { it.destination is ComposeNavigator.Destination }
            .mapNotNull { it.destination.route }

    private fun assertLandedOnlyOn(controller: NavHostController, home: String) {
        composeRule.runOnIdle {
            assertEquals("exactly one visible entry — $home", listOf(home), composeRoutes(controller))
            assertEquals(home, controller.currentDestination?.route)
            assertFalse(
                "booking sub-graph is gone",
                controller.currentBackStack.value.any { it.destination.route == "bookingGraph" },
            )
        }
    }

    // A — Customer: [home, salonList, bookingGraph{...bookingSuccess}] → [home]
    @Test
    fun customerBookingSuccess_leavesOnlyHome() {
        val controller = nav(startRoute = "home")
        push(controller, "salonList", "bookingStart", "bookingCustomer", "bookingSuccess")
        composeRule.runOnUiThread { controller.navigateHomeAfterBooking("home") }
        assertLandedOnlyOn(controller, "home")
    }

    // B — Manager: [dashboard, bookingGraph{...}] → [dashboard]
    @Test
    fun managerBookingSuccess_leavesOnlyDashboard() {
        val controller = nav(startRoute = "dashboard")
        push(controller, "bookingStart", "bookingCustomer", "bookingSuccess")
        composeRule.runOnUiThread { controller.navigateHomeAfterBooking("dashboard") }
        assertLandedOnlyOn(controller, "dashboard")
    }

    // C — Reception: same shape as Manager
    @Test
    fun receptionBookingSuccess_leavesOnlyDashboard() {
        val controller = nav(startRoute = "dashboard")
        push(controller, "bookingStart", "bookingSuccess")
        composeRule.runOnUiThread { controller.navigateHomeAfterBooking("dashboard") }
        assertLandedOnlyOn(controller, "dashboard")
    }

    // D — Customer "log in while booking": start on EXPLORE, home never on the
    //     stack, booking flow present → must still end on a single home with
    //     the booking sub-graph (and its scoped ViewModel) gone.
    @Test
    fun customerLoginWhileBooking_clearsBookingFlowAndLandsOnHome() {
        val controller = nav(startRoute = "explore")
        push(controller, "salonList", "bookingStart", "bookingCustomer", "bookingSuccess")
        // pre-condition: home is genuinely absent
        composeRule.runOnIdle {
            assertFalse("home not on the stack yet", composeRoutes(controller).contains("home"))
        }
        composeRule.runOnUiThread { controller.navigateHomeAfterBooking("home") }
        assertLandedOnlyOn(controller, "home")
    }
}
