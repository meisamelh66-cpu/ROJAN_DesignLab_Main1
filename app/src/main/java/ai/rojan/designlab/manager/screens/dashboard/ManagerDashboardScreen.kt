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
import ai.rojan.designlab.manager.presentation.dashboard.ManagerDashboardViewModel
import ai.rojan.designlab.manager.presentation.dashboard.ManagerDashboardViewModelFactory
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.ui.theme.RojanDimens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/**
 * Manager App workspace — Dashboard v1.0 UI.
 *
 * **TEAM2-002 (Manager Data Persistence):** [SalonIdentityCard] and
 * [TodayOverviewSection] now render the authenticated manager's real
 * salon and real today's-bookings stats via [ManagerDashboardViewModel]
 * (`GET /api/v1/salons/mine` + the salon's real bookings) — replacing
 * `SalonIdentityCard`'s hardcoded default params and
 * `TodayOverviewSection`'s previous internal
 * `manager.data.computeManagerDashboardStats()`/`ManagerRepositories`
 * read. [QuickActionsSection]/[AIInsightCard]/[CalendarPreviewSection]'s
 * own content is unchanged — this task's scope is data persistence for
 * dashboard/calendar/status-update, not a rewrite of every section.
 * [onRequireLogin] fires when a real 401 means the stored session is
 * genuinely dead (not just retriable) — see
 * [ManagerDashboardViewModel.requiresReauth]'s doc comment.
 */
@Composable
fun ManagerDashboardScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onViewCalendarClick: () -> Unit = {},
    onCreateAppointmentClick: () -> Unit = {},
    onViewCustomersClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
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
                            else -> Unit
                        }
                    },
                )
            }
            item { AIInsightCard() }
            item { CalendarPreviewSection(onViewCalendarClick = onViewCalendarClick) }
        }
    }
}
