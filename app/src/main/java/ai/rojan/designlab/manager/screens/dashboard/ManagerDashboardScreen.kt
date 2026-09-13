package ai.rojan.designlab.manager.screens.dashboard

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.manager.components.AIInsightCard
import ai.rojan.designlab.manager.components.CalendarPreviewSection
import ai.rojan.designlab.manager.components.ManagerEmptyState
import ai.rojan.designlab.manager.components.ManagerErrorState
import ai.rojan.designlab.manager.components.ManagerHeader
import ai.rojan.designlab.manager.components.ManagerLoadingState
import ai.rojan.designlab.manager.components.ManagerQuickAction
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.components.QuickActionsSection
import ai.rojan.designlab.manager.components.SalonIdentityCard
import ai.rojan.designlab.manager.components.TodayOverviewSection
import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.domain.ai.ManagerCrmInsightCategory
import ai.rojan.designlab.manager.presentation.dashboard.ManagerDashboardViewModel
import ai.rojan.designlab.manager.presentation.dashboard.ManagerDashboardViewModelFactory
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.ui.theme.RojanDimens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/**
 * Manager App workspace — Dashboard v1.0 UI.
 *
 * Real entry points wired in [ai.rojan.designlab.manager.navigation.ManagerNavGraph]:
 * [onViewCalendarClick] (Calendar Preview CTA), [onCreateAppointmentClick]
 * (Quick Actions' "نوبت جدید", routes into the booking wizard),
 * [onViewCustomersClick] (Quick Actions' "مشتری جدید", routes to
 * [ai.rojan.designlab.manager.screens.customers.ManagerCustomersListScreen]
 * — the only existing customers destination; there is no separate
 * customer-creation screen), [onViewServicesClick] (Quick Actions'
 * "خدمات", routes to [ai.rojan.designlab.manager.screens.services.ManagerServicesScreen] —
 * Manager Operational Foundation, Phase 6 Step 1), [onViewStaffClick]
 * (Quick Actions' "کارکنان", routes to
 * [ai.rojan.designlab.manager.screens.staff.ManagerStaffScreen] — Phase 6
 * Step 2), [onProfileClick] (header greeting, routes to
 * [ai.rojan.designlab.manager.screens.profile.ManagerProfileScreen]), and
 * [onSettingsClick] (Quick Actions' "تنظیمات", routes to
 * [ai.rojan.designlab.manager.screens.settings.ManagerSalonSetupScreen] —
 * First Salon Pilot, Phase A). The `when` below is exhaustive over all
 * five [ManagerQuickAction] values, so no `else` branch remains. All
 * default to no-op so this screen stays navigation-agnostic standalone.
 *
 * AI Insight Presentation Layer, Phase 7 Step 4: [onViewInactiveCustomersClick]
 * is [AIInsightCard]'s inactive-customer-count line, routing to
 * [ai.rojan.designlab.manager.screens.customers.ManagerCustomersListScreen]
 * pre-filtered to [ai.rojan.designlab.manager.domain.customer.CustomerTag.INACTIVE] -
 * distinct from [onViewCustomersClick] (unfiltered), since they land on
 * different filter states of the same screen. The count is filtered to
 * [ManagerCrmInsightCategory.INACTIVE_CUSTOMER] - otherwise this label
 * (fixed text "X مشتری غیرفعال") would start counting VIP insights too.
 *
 * **TEAM2-002 (Manager Data Persistence, reconciled with the above):**
 * [SalonIdentityCard] and [TodayOverviewSection] now render the
 * authenticated manager's real salon and real today's-bookings stats via
 * [ManagerDashboardViewModel] (`GET /api/v1/salons/mine` + the salon's
 * real bookings) — replacing `SalonIdentityCard`'s hardcoded default
 * params and `TodayOverviewSection`'s previous internal
 * `manager.data.computeManagerDashboardStats()`/`ManagerRepositories`
 * read (that file, and its `refreshKey`-driven re-trigger, are retired).
 * [AIInsightCard]/[CalendarPreviewSection]'s own data sourcing is
 * genuinely unchanged by that move — [AIInsightCard] still reads
 * [ManagerRepositories.dashboardInsights]/[ManagerRepositories.crmInsights]
 * directly (a separate concern from the ViewModel above, still needing
 * its own [ManagerRepositories.initialize] sync trigger below), and
 * [CalendarPreviewSection] still takes no real slots (its own
 * `computeTodaysUpcomingSlots()` source was retired along with
 * `ManagerDashboardStats.kt`; wiring a real replacement over
 * [ai.rojan.designlab.domain.repository.BookingRepository.salonBookings]
 * is a genuine follow-up, not invented here). [onRequireLogin] fires when
 * a real 401 means the stored session is genuinely dead (not just
 * retriable) — see [ManagerDashboardViewModel.requiresReauth]'s doc
 * comment.
 */
@Composable
fun ManagerDashboardScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onViewCalendarClick: () -> Unit = {},
    onCreateAppointmentClick: () -> Unit = {},
    onViewCustomersClick: () -> Unit = {},
    onViewInactiveCustomersClick: () -> Unit = {},
    onViewServicesClick: () -> Unit = {},
    onViewStaffClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onRequireLogin: () -> Unit = {},
    viewModel: ManagerDashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = ManagerDashboardViewModelFactory(
            salonRepository = BackendApiContainerHolder.get(LocalContext.current).salonRepository,
            bookingRepository = BackendApiContainerHolder.get(LocalContext.current).bookingRepository,
            specialistRepository = BackendApiContainerHolder.get(LocalContext.current).specialistRepository,
            serviceCategoryRepository = BackendApiContainerHolder.get(LocalContext.current).serviceCategoryRepository,
            serviceRepository = BackendApiContainerHolder.get(LocalContext.current).serviceRepository,
        ),
    ),
) {
    val context = LocalContext.current

    // AIInsightCard's data (crmInsights/dashboardInsights) still comes
    // from the pre-existing ManagerRepositories singleton, untouched by
    // the Dashboard's move onto ManagerDashboardViewModel above — still
    // needs its own sync trigger. Failure is swallowed here deliberately,
    // same as before: AIInsightCard simply keeps showing an empty state
    // rather than crashing the dashboard over a network error on load.
    LaunchedEffect(Unit) {
        ManagerRepositories.initialize(context)
    }

    LaunchedEffect(viewModel.requiresReauth) {
        if (viewModel.requiresReauth) onRequireLogin()
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

            when (val dashboardState = viewModel.state) {
                is UiState.Loading -> item { ManagerLoadingState(message = "در حال بارگذاری اطلاعات سالن...") }
                is UiState.Error -> item {
                    ManagerErrorState(
                        description = dashboardState.message,
                        actionLabel = "تلاش مجدد",
                        onAction = { viewModel.retry() },
                    )
                }
                is UiState.Empty -> item {
                    ManagerEmptyState(
                        title = "هنوز سالنی ثبت نکرده‌اید",
                        description = "برای استفاده از پنل مدیریت، ابتدا باید یک سالن برای حساب کاربری خود ثبت کنید.",
                    )
                }
                is UiState.Success -> {
                    val data = dashboardState.data
                    item {
                        SalonIdentityCard(
                            salonName = data.salonName,
                            salonCategory = data.salonDescription ?: "زیبایی و سلامت",
                            isActive = data.isActive,
                        )
                    }
                    item { TodayOverviewSection(stats = data.stats) }
                }
            }

            item {
                QuickActionsSection(
                    onActionClick = { action ->
                        when (action) {
                            ManagerQuickAction.NEW_APPOINTMENT -> onCreateAppointmentClick()
                            ManagerQuickAction.NEW_CUSTOMER -> onViewCustomersClick()
                            ManagerQuickAction.SERVICES -> onViewServicesClick()
                            ManagerQuickAction.STAFF -> onViewStaffClick()
                            ManagerQuickAction.SETTINGS -> onSettingsClick()
                        }
                    },
                )
            }
            item {
                AIInsightCard(
                    message = ManagerRepositories.dashboardInsights?.topRecommendationMessage,
                    inactiveCustomerCount = ManagerRepositories.crmInsights.count { it.category == ManagerCrmInsightCategory.INACTIVE_CUSTOMER },
                    onInactiveCustomersClick = onViewInactiveCustomersClick,
                )
            }
            item {
                CalendarPreviewSection(onViewCalendarClick = onViewCalendarClick)
            }
        }
    }
}
