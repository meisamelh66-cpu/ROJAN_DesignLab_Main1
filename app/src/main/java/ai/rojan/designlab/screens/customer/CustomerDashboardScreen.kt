package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import ai.rojan.designlab.components.hero.HeroBookingCard
import ai.rojan.designlab.presentation.auth.AuthViewModel
import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.ui.background.WarmBackground
import ai.rojan.designlab.ui.theme.RojanDimens

/**
 * ROJAN AI Customer Home Dashboard — UX Correction (Explore Repositioning).
 *
 * The customer's real first impression after Splash → Authentication,
 * per explicit correction: the previous first screen
 * ([ai.rojan.designlab.navigation.RojanDestinations.CUSTOMER_HOME], now
 * [CustomerHomeScreen] at [ai.rojan.designlab.navigation.RojanDestinations.EXPLORE])
 * led with marketplace discovery (search bar, service/specialist/salon
 * browsing) — too much "go shop" and not enough "welcome back" for a
 * landing screen. This screen prioritizes exactly the five things asked
 * for, reusing existing components unchanged (no new visual design):
 * personalized greeting ([HomeHeader]), premium booking hero CTA
 * ([HeroBookingCard]), upcoming appointments ([UpcomingBookings]), AI
 * recommendations ([RecommendedSalons]), and suggested specialists
 * ([TopSpecialists]). Marketplace discovery is one tap away via
 * [onExploreClick] (bottom bar's SEARCH tab), not removed — see
 * [CustomerHomeScreen]'s own doc comment.
 *
 * Same shell as [CustomerHomeScreen] ([WarmBackground] + [LazyColumn] +
 * [RojanDimens.SpaceLG] rhythm + [CustomerBottomBar]) for visual
 * consistency with the frozen Design Baseline v1.0 — this is a
 * navigation/composition change, not a new design.
 */
@Composable
fun CustomerDashboardScreen(
    ecosystemViewModel: CustomerEcosystemViewModel,
    authViewModel: AuthViewModel,
    onProfileClick: () -> Unit = {},
    onBookAppointmentClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    onSalonClick: (String) -> Unit = {},
) {
    WarmBackground(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceLG),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceLG),
        ) {
            item { HomeHeader(displayName = authViewModel.currentDisplayName) }
            item { HeroBookingCard(onClick = onBookAppointmentClick) }
            item { UpcomingBookings(ecosystemViewModel) }
            item { RecommendedSalons(onSalonClick = onSalonClick) }
            item { TopSpecialists() }
            item {
                CustomerBottomBar(
                    activeTab = CustomerHomeTab.HOME,
                    onTabSelected = { tab ->
                        when (tab) {
                            CustomerHomeTab.HOME -> Unit
                            CustomerHomeTab.SEARCH -> onExploreClick()
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
