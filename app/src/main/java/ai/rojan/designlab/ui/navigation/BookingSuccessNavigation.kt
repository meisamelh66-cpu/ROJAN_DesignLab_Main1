package ai.rojan.designlab.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.navOptions

/**
 * 5B7-2 — return to the flavor's home destination after a **completed**
 * booking.
 *
 * The old wiring (`navigate(home) { popUpTo(home) { inclusive = false } }`,
 * no `launchSingleTop`) left `home` on the stack and then pushed a second
 * `home` — `[home, home]` — so Back from the post-booking home returned to
 * a stale home instead of exiting. On the Customer "log in while booking"
 * path it was worse: `home` (`CUSTOMER_HOME`) was never on the stack (the
 * frozen `startDestination` is `EXPLORE`), so `popUpTo(home)` matched
 * nothing and the entire finished booking sub-graph — including its
 * scoped `BookingViewModel` — stayed alive under the new `home`.
 *
 * This clears the **whole** back stack (`popUpTo(graph.id) { inclusive =
 * true }`) and lands on a single `home`. Popping the graph id itself,
 * rather than the logout convention's `findStartDestination().id`, is
 * deliberate: the frozen graph start may no longer be on the stack
 * (Manager/Reception replace it during the auth → dashboard transition;
 * Customer's is `EXPLORE`), whereas the graph id always clears every
 * entry. Popping the whole stack also disposes the booking sub-graph's
 * `NavBackStackEntry`, which is what actually clears the booking
 * ViewModel.
 */
fun NavController.navigateHomeAfterBooking(homeRoute: String) {
    navigate(homeRoute, bookingSuccessNavOptions(graph.id))
}

/** The [NavOptions] [navigateHomeAfterBooking] applies — extracted so it is unit-testable without a hosted graph. */
internal fun bookingSuccessNavOptions(graphId: Int): NavOptions = navOptions {
    popUpTo(graphId) { inclusive = true }
    launchSingleTop = true
}
