package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import ai.rojan.designlab.components.hero.HeroBookingCard
import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.ui.background.PremiumBackground
import ai.rojan.designlab.ui.theme.RojanDimens

/**
 * ROJAN AI Customer Home Screen.
 *
 * Rendering-integration fix (see the accompanying fix report): this
 * composable previously rendered a hardcoded debug list —
 * `Text("HomeHeader")`, `Text("AISearchBar")`, etc. — literal strings
 * naming each section instead of actually calling the section
 * composables. That's corrected below: every section is now a real call
 * to its corresponding composable, in the approved layout hierarchy
 * (Header → AI Search → Hero Booking → Categories → Featured Content →
 * AI Recommendations → Bottom Navigation), wrapped in [PremiumBackground]
 * so the screen background system is actually applied (it wasn't before).
 *
 * [LazyColumn] replaces the previous fixed [Column] — with 13 stacked
 * sections, a non-scrolling Column would overflow the screen the moment
 * any section has real content; this is a rendering-correctness fix, not
 * a design change.
 *
 * Booking Experience Refactor, spec section 2 — Home Screen: "Remove
 * service categories from the Home screen... Service categories do NOT
 * belong on the Home page." [ServiceCategories] removed from this
 * composition (categories now only appear after Book Appointment, per
 * spec section 7 — that's [ai.rojan.designlab.screens.booking.ServiceCategoriesScreen]).
 * [HeroBookingCard]'s CTA now starts the Auth flow (spec section 3).
 *
 * Code Cleanup pass: [ecosystemViewModel] now threaded through to the 3
 * sections that need real customer state
 * ([UpcomingBookings]/[RecentVisits]/[FavoriteSalons]) — a freshly-
 * constructed instance scoped to this screen's own backstack entry, not
 * the same instance `PROFILE_GRAPH` uses (they're separate top-level
 * routes, not nested) — same real data source class and real demo data
 * either way, just not live-synced within one session unless the user
 * navigates back through. All [CustomerBottomBar] tabs are now wired:
 * HOME is a no-op (already on Home), BOOKINGS/FAVORITES/PROFILE navigate
 * to their real destinations — no inactive tabs remain.
 *
 * Honest note carried over from the previous fix report: [ServiceCategories]
 * itself is no longer part of this composition at all (removed per spec
 * section 2, not merely "still empty") — the rest of this note applied
 * to a since-resolved rendering bug and no longer describes the current
 * state; kept only as history in version control, not restated here.
 */
@Composable
fun CustomerHomeScreen(
    ecosystemViewModel: CustomerEcosystemViewModel,
    onProfileClick: () -> Unit = {},
    onBookAppointmentClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
) {
    PremiumBackground(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item { HomeHeader() }
            item { AISearchBar() }
            item { HeroBookingCard(onClick = onBookAppointmentClick) }
            item { FeaturedSalons() }
            item { TopSpecialists() }
            item { PromotionsSection() }
            item { NearbySalons() }
            item { RecommendedServices() }
            item { UpcomingBookings(ecosystemViewModel) }
            item { RecentVisits(ecosystemViewModel) }
            item { FavoriteSalons(ecosystemViewModel) }
            item {
                CustomerBottomBar(
                    onTabSelected = { tab ->
                        when (tab) {
                            CustomerHomeTab.HOME -> Unit
                            CustomerHomeTab.BOOKINGS -> onBookingsClick()
                            CustomerHomeTab.FAVORITES -> onFavoritesClick()
                            CustomerHomeTab.PROFILE -> onProfileClick()
                        }
                    }
                )
            }
        }
    }
}
