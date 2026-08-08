package ai.rojan.designlab.manager.screens.dashboard

import ai.rojan.designlab.manager.components.AIInsightCard
import ai.rojan.designlab.manager.components.CalendarPreviewSection
import ai.rojan.designlab.manager.components.ManagerHeader
import ai.rojan.designlab.manager.components.ManagerQuickAction
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.components.QuickActionsSection
import ai.rojan.designlab.manager.components.SalonIdentityCard
import ai.rojan.designlab.manager.components.TodayOverviewSection
import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.data.computeTodaysUpcomingSlots
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview

/**
 * Manager App workspace — Dashboard v1.0 UI. Isolated from Customer App
 * v1.0 (frozen): lives entirely under `ai.rojan.designlab.manager`, built
 * only from shared design-system primitives — no Customer screen/
 * component/route is read from or modified. Distinct from the
 * pre-existing, unrelated placeholder at
 * [ai.rojan.designlab.screens.dashboard.ManagerDashboardScreen] (a
 * different package, part of Customer's own role-based dashboards).
 *
 * Real entry points wired in [ai.rojan.designlab.manager.navigation.ManagerNavGraph]:
 * [onViewCalendarClick] (Calendar Preview CTA), [onCreateAppointmentClick]
 * (Quick Actions' "نوبت جدید", routes into the booking wizard),
 * [onViewCustomersClick] (Quick Actions' "مشتری جدید", routes to
 * [ai.rojan.designlab.manager.screens.customers.ManagerCustomersListScreen]
 * — the only existing customers destination; there is no separate
 * customer-creation screen), and [onProfileClick] (header greeting,
 * routes to [ai.rojan.designlab.manager.screens.profile.ManagerProfileScreen]).
 * All default to no-op so this screen stays navigation-agnostic
 * standalone/in `@Preview`. The remaining Quick Actions
 * (خدمات/کارکنان/تنظیمات) have no implemented screen yet, so
 * [QuickActionsSection] renders them disabled rather than wiring them to
 * a placeholder. Phase 2, M6: [SalonIdentityCard]/[AIInsightCard]/
 * [CalendarPreviewSection] are now wired to real backend data too
 * ([ManagerRepositories.salon]/[ManagerRepositories.dashboardInsights]/
 * [computeTodaysUpcomingSlots]) — see those components for the exact
 * shape.
 */
@Composable
fun ManagerDashboardScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onViewCalendarClick: () -> Unit = {},
    onCreateAppointmentClick: () -> Unit = {},
    onViewCustomersClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }

    // Manager App Phase 2: resolves the owner's real salon and syncs real Service/Appointment
    // data (see ManagerRepositories.initialize's own doc comment). Runs once per screen entry;
    // failure is swallowed here deliberately - TodayOverviewSection/Calendar simply keep showing
    // an empty state rather than crashing the dashboard over a network error on load.
    LaunchedEffect(Unit) {
        ManagerRepositories.initialize(context)
        refreshKey++
    }

    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Shared Premium Glass Design System spacing rhythm: compact
            // section-to-section gap so stacked cards read as one
            // dashboard, not isolated islands with large empty gaps.
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSectionToSection),
        ) {
            item { ManagerHeader(onProfileClick = onProfileClick) }
            item {
                val salon = ManagerRepositories.salon
                if (salon != null) {
                    SalonIdentityCard(salonName = salon.name, salonCategory = salon.description, isActive = salon.active)
                } else {
                    SalonIdentityCard()
                }
            }
            item { TodayOverviewSection(refreshKey = refreshKey) }
            item {
                QuickActionsSection(
                    onActionClick = { action ->
                        when (action) {
                            ManagerQuickAction.NEW_APPOINTMENT -> onCreateAppointmentClick()
                            ManagerQuickAction.NEW_CUSTOMER -> onViewCustomersClick()
                            else -> Unit
                        }
                    },
                )
            }
            item { AIInsightCard(message = ManagerRepositories.dashboardInsights?.topRecommendationMessage) }
            item {
                val slots = remember(refreshKey) { computeTodaysUpcomingSlots() }
                CalendarPreviewSection(slots = slots, onViewCalendarClick = onViewCalendarClick)
            }
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
)
@Composable
private fun ManagerDashboardScreenPreview() {
    RojanTheme {
        ManagerDashboardScreen()
    }
}
