package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

import ai.rojan.designlab.components.hero.HeroBookingCard
import ai.rojan.designlab.presentation.auth.AuthViewModel
import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.ui.theme.RojanDimens

/**
 * ROJAN AI Customer Home Screen.
 *
 * Rendering-integration fix (see the accompanying fix report): this
 * composable previously rendered a hardcoded debug list —
 * `Text("HomeHeader")`, `Text("AISearchBar")`, etc. — literal strings
 * naming each section instead of actually calling the section
 * composables. That's corrected below: every section is now a real call
 * to its corresponding composable, wrapped in [WarmBackground] so the
 * screen background system is actually applied (it wasn't before).
 *
 * Home Premium Polish Phase: section order now matches the approved
 * reference's explicit sequence — Header → Search → Services/Salons tabs
 * → Popular Services → Top Specialists → Promotion Banner → Featured
 * Salons — with the remaining sections the reference doesn't call out
 * (Hero Booking, Nearby/Recommended/Upcoming/Recent/Followed Salons)
 * kept afterward in their prior relative order; no section was removed.
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
 *
 * Luxury Visual Refinement Phase: outer padding and inter-section spacing
 * both raised (SpaceMD -> SpaceLG) for more breathing room between
 * sections — a boutique app shouldn't feel edge-to-edge dense.
 *
 * UX Correction (Explore Repositioning): this screen's own content is
 * unchanged — same 13 sections, same order, same components — but it no
 * longer renders at the [ai.rojan.designlab.navigation.RojanDestinations.CUSTOMER_HOME]
 * route. [ai.rojan.designlab.screens.customer.CustomerDashboardScreen] is
 * the new first-impression screen after auth; this one now renders at
 * [ai.rojan.designlab.navigation.RojanDestinations.EXPLORE] as the
 * marketplace/discovery destination reached from there. [onHomeClick]
 * is new (bottom bar's HOME tab was a no-op "already on Home" — no longer
 * true now that this isn't Home) — everything else about this screen is
 * untouched.
 */
@Composable
fun CustomerHomeScreen(
    ecosystemViewModel: CustomerEcosystemViewModel,
    authViewModel: AuthViewModel,
    onProfileClick: () -> Unit = {},
    onBookAppointmentClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSalonClick: (String) -> Unit = {},
    onHomeClick: () -> Unit = {},
    onViewAllServicesClick: () -> Unit = {},
    onSpecialistClick: (String) -> Unit = {},
    // Routing-identity fix: this same screen renders in two different
    // contexts — as the new/unauthenticated user's actual Landing screen
    // (reached directly from Splash, no prior back-stack entry) and as the
    // destination of the Dashboard's "جستجو" (Search) tab. A single
    // hardcoded activeTab couldn't be correct for both; the caller now
    // decides which tab reads as active based on how this screen was
    // reached, not this composable itself.
    bottomBarActiveTab: CustomerHomeTab = CustomerHomeTab.SEARCH,
) {
    var searchMode by remember { mutableStateOf(SearchMode.SERVICES) }

    HomeBackgroundTheme(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceLG),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceLG),
        ) {
            item {
                HomeHeader(
                    displayName = authViewModel.currentDisplayName,
                    onProfileClick = onProfileClick,
                )
            }
            item { AISearchBar(onClick = onSearchClick) }
            item {
                SearchModeTabs(
                    selected = searchMode,
                    onSelectedChange = { searchMode = it },
                )
            }
            item { PopularServices(onViewAllClick = onViewAllServicesClick) }
            item { TopSpecialists(onSpecialistClick = onSpecialistClick) }
            item { PromotionsSection() }
            item { FeaturedSalons() }
            item { HeroBookingCard(onClick = onBookAppointmentClick) }
            item { NearbySalons() }
            item { RecommendedSalons(onSalonClick = onSalonClick) }
            item { UpcomingBookings(ecosystemViewModel) }
            item { RecentVisits(ecosystemViewModel, onSalonClick = onSalonClick) }
            item { FollowedSalons(ecosystemViewModel, onSalonClick = onSalonClick) }
            item {
                CustomerBottomBar(
                    activeTab = bottomBarActiveTab,
                    onTabSelected = { tab ->
                        when (tab) {
                            CustomerHomeTab.HOME -> onHomeClick()
                            CustomerHomeTab.SEARCH -> onSearchClick()
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
