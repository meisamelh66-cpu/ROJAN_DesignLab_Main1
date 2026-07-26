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
 * composition. (UX Refactor Phase 1: the category-first pre-booking
 * screen this note originally pointed to has since been deleted
 * entirely — both target flows pick the salon before narrowing
 * services, not the other way around.)
 * [HeroBookingCard]'s CTA now starts salon browsing (UX Refactor Phase 1
 * repointed it from the Auth flow — see [onBookAppointmentClick]).
 *
 * Code Cleanup pass: [ecosystemViewModel] now threaded through to the 3
 * sections that need real customer state
 * ([UpcomingBookings]/[RecentVisits]/[FollowedSalons]) — a freshly-
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
 *
 * UX Refactor Phase 1: [onSearchClick] and [onSalonClick] wire up the
 * previously-dead [AISearchBar] and the "Followed Salons" / "Previous
 * Salons" / "ROJAN AI Recommended Salons" sections the target Customer
 * Home spec calls for — [FollowedSalons] (renamed from FavoriteSalons),
 * [RecentVisits] (relabeled "Previous Salons"), and [RecommendedSalons]
 * (renamed from RecommendedServices, now recommending salons instead of
 * services). Sections not named in that spec (Hero/Featured/Top
 * Specialists/Promotions) are left as-is — out of this phase's scope.
 */
@Composable
fun CustomerHomeScreen(
    ecosystemViewModel: CustomerEcosystemViewModel,
    onProfileClick: () -> Unit = {},
    onBookAppointmentClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSalonClick: (String) -> Unit = {},
) {
    PremiumBackground(
        modifier = Modifier.fillMaxSize(),
        softenForContent = true,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item { HomeHeader() }
            item { AISearchBar(onClick = onSearchClick) }
            item { HeroBookingCard(onClick = onBookAppointmentClick) }
            item { FeaturedSalons() }
            item { TopSpecialists() }
            item { PromotionsSection() }
            item { NearbySalons() }
            item { RecommendedSalons(onSalonClick = onSalonClick) }
            item { UpcomingBookings(ecosystemViewModel) }
            item { RecentVisits(ecosystemViewModel, onSalonClick = onSalonClick) }
            item { FollowedSalons(ecosystemViewModel, onSalonClick = onSalonClick) }
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
