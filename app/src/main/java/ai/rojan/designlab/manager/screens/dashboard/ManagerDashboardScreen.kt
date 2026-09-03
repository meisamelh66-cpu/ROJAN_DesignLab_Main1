package ai.rojan.designlab.manager.screens.dashboard

import ai.rojan.designlab.manager.components.AIInsightCard
import ai.rojan.designlab.manager.components.CalendarPreviewSection
import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerHeader
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerQuickAction
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.components.QuickActionsSection
import ai.rojan.designlab.manager.components.SalonIdentityCard
import ai.rojan.designlab.manager.components.TodayOverviewSection
import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.data.computeTodaysUpcomingSlots
import ai.rojan.designlab.manager.domain.ai.ManagerCrmInsightCategory
import ai.rojan.designlab.manager.presentation.dashboard.ManagerDashboardUiState
import ai.rojan.designlab.manager.presentation.dashboard.ManagerDashboardViewModel
import ai.rojan.designlab.manager.presentation.dashboard.ManagerDashboardViewModelFactory
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTheme
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

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
 * customer-creation screen), [onViewServicesClick] (Quick Actions'
 * "خدمات", routes to [ai.rojan.designlab.manager.screens.services.ManagerServicesScreen] —
 * Manager Operational Foundation, Phase 6 Step 1), [onViewStaffClick]
 * (Quick Actions' "کارکنان", routes to
 * [ai.rojan.designlab.manager.screens.staff.ManagerStaffScreen] — Phase 6
 * Step 2), [onProfileClick] (header greeting, routes to
 * [ai.rojan.designlab.manager.screens.profile.ManagerProfileScreen]), and
 * [onSettingsClick] (Quick Actions' "تنظیمات", routes to
 * [ai.rojan.designlab.manager.screens.settings.ManagerSalonSetupScreen] —
 * First Salon Pilot, Phase A). Previously this chip had no handler wired
 * (see [QuickActionsSection]'s own doc comment, now stale for this one
 * case); the `when` below is exhaustive over all five [ManagerQuickAction]
 * values now, so no `else` branch remains.
 * All default to no-op so this screen stays navigation-agnostic
 * standalone/in `@Preview`.
 * Phase 2, M6: [SalonIdentityCard]/[AIInsightCard]/
 * [CalendarPreviewSection] are now wired to real backend data too
 * ([ManagerRepositories.salon]/[ManagerRepositories.dashboardInsights]/
 * [computeTodaysUpcomingSlots]) — see those components for the exact
 * shape.
 *
 * AI Insight Presentation Layer, Phase 7 Step 4: [onViewInactiveCustomersClick]
 * is [AIInsightCard]'s new inactive-customer-count line, routing to
 * [ai.rojan.designlab.manager.screens.customers.ManagerCustomersListScreen]
 * pre-filtered to [ai.rojan.designlab.manager.domain.customer.CustomerTag.INACTIVE] -
 * distinct from [onViewCustomersClick] (unfiltered), since they land on
 * different filter states of the same screen. Frozen Dashboard baseline
 * untouched otherwise: section order/count and [AIInsightCard]'s existing
 * layout are unchanged - see that component's own doc comment for what
 * was added inside it.
 *
 * Phase 7 Step 5: [ManagerRepositories.crmInsights] can now hold more
 * than one kind of insight ([ai.rojan.designlab.manager.domain.ai.VipCustomerInsightProvider]
 * joined [ai.rojan.designlab.manager.domain.ai.InactiveCustomerInsightProvider]
 * via [ai.rojan.designlab.manager.domain.ai.CompositeManagerCrmInsightProvider]),
 * so the count passed here is filtered to
 * [ManagerCrmInsightCategory.INACTIVE_CUSTOMER] - otherwise this label
 * (fixed text "X مشتری غیرفعال") would start counting VIP insights too.
 *
 * Reliability Sprint 01, P0-1: initialization now goes through
 * [ManagerDashboardViewModel] instead of a bare `LaunchedEffect` that
 * discarded [ManagerRepositories.initialize]'s [Result] — a real load
 * failure now renders [ManagerDashboardUiState.Error] with a retry
 * action, rather than silently rendering the same empty state a
 * genuinely-zero-activity salon would show. See [ManagerDashboardContent]
 * for the state dispatch.
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
) {
    val context = LocalContext.current
    val viewModel: ManagerDashboardViewModel = viewModel(
        factory = ManagerDashboardViewModelFactory(context.applicationContext),
    )

    ManagerDashboardContent(
        uiState = viewModel.state,
        onRetry = viewModel::retry,
        modifier = modifier,
        onBackClick = onBackClick,
        onViewCalendarClick = onViewCalendarClick,
        onCreateAppointmentClick = onCreateAppointmentClick,
        onViewCustomersClick = onViewCustomersClick,
        onViewInactiveCustomersClick = onViewInactiveCustomersClick,
        onViewServicesClick = onViewServicesClick,
        onViewStaffClick = onViewStaffClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
    )
}

/**
 * Stateless content split out from [ManagerDashboardScreen] so the three
 * [ManagerDashboardUiState] branches can be previewed directly with a
 * hardcoded state — no [ManagerDashboardViewModel]/[LocalContext]/network
 * needed (see the three `@Preview`s below).
 */
@Composable
private fun ManagerDashboardContent(
    uiState: ManagerDashboardUiState,
    onRetry: () -> Unit,
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
) {
    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        when (uiState) {
            is ManagerDashboardUiState.Loading -> ManagerDashboardLoading()
            is ManagerDashboardUiState.Error -> ManagerDashboardError(message = uiState.message, onRetry = onRetry)
            is ManagerDashboardUiState.Ready -> {
                val salon by ManagerRepositories.salon.collectAsStateWithLifecycle()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // Shared Premium Glass Design System spacing rhythm: compact
                    // section-to-section gap so stacked cards read as one
                    // dashboard, not isolated islands with large empty gaps.
                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSectionToSection),
                ) {
                    item { ManagerHeader(onProfileClick = onProfileClick) }
                    item {
                        val currentSalon = salon
                        if (currentSalon != null) {
                            SalonIdentityCard(
                                salonName = currentSalon.name,
                                salonCategory = currentSalon.description,
                                isActive = currentSalon.active,
                            )
                        } else {
                            SalonIdentityCard(isLoading = true)
                        }
                    }
                    item { TodayOverviewSection() }
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
                        val slots = remember { computeTodaysUpcomingSlots() }
                        CalendarPreviewSection(slots = slots, onViewCalendarClick = onViewCalendarClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagerDashboardLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ManagerColors.Turquoise)
    }
}

/** Mirrors [ai.rojan.designlab.manager.screens.auth.ManagerAccessErrorScreen]'s existing card+retry pattern — same components, same shape, the proven Manager-palette-correct precedent for this exact failure class. */
@Composable
private fun ManagerDashboardError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RojanDimens.SpaceLG),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "بارگذاری داشبورد با خطا مواجه شد",
                    style = RojanTypography.CardTitle,
                    color = ManagerColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = message,
                    style = RojanTypography.Body,
                    color = ManagerColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = RojanDimens.SpaceSM),
                )
            }
        }

        ManagerPrimaryButton(
            text = "تلاش مجدد",
            onClick = onRetry,
            modifier = Modifier.padding(top = RojanDimens.SpaceLG),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ManagerDashboardScreenLoadingPreview() {
    RojanTheme {
        ManagerDashboardContent(uiState = ManagerDashboardUiState.Loading, onRetry = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ManagerDashboardScreenErrorPreview() {
    RojanTheme {
        ManagerDashboardContent(
            uiState = ManagerDashboardUiState.Error("اتصال به اینترنت برقرار نیست"),
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ManagerDashboardScreenReadyPreview() {
    RojanTheme {
        ManagerDashboardContent(uiState = ManagerDashboardUiState.Ready, onRetry = {})
    }
}
