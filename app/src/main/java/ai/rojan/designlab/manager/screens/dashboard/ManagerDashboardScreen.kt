package ai.rojan.designlab.manager.screens.dashboard

import ai.rojan.designlab.manager.components.AIInsightCard
import ai.rojan.designlab.manager.components.CalendarPreviewSection
import ai.rojan.designlab.manager.components.ManagerHeader
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.components.QuickActionsSection
import ai.rojan.designlab.manager.components.SalonIdentityCard
import ai.rojan.designlab.manager.components.TodayOverviewSection
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * Manager App workspace — Dashboard v1.0 UI. Isolated from Customer App
 * v1.0 (frozen): lives entirely under `ai.rojan.designlab.manager`, built
 * only from shared design-system primitives ([ManagerScaffold] wraps
 * the shared `WarmBackground`, the glass system,
 * [ai.rojan.designlab.ui.theme.RojanTypography]/tokens, the same brand
 * mark Customer App uses) — no Customer screen/component/route is read
 * from or modified.
 *
 * UI only, per this pass's scope: every section below ([ManagerHeader],
 * [SalonIdentityCard], [TodayOverviewSection], [QuickActionsSection],
 * [AIInsightCard], [CalendarPreviewSection]) renders static placeholder
 * content — no backend, no ViewModel, no navigation wiring (this screen
 * is still not called from anywhere; see
 * [ai.rojan.designlab.manager.navigation.ManagerNavGraph]). Distinct
 * from the pre-existing, already-routed placeholder at
 * [ai.rojan.designlab.screens.dashboard.ManagerDashboardScreen].
 *
 * Readability theme update: uses [ManagerScaffold] (warm white
 * background) instead of the shared [ai.rojan.designlab.ui.components.scaffold.RojanScaffold]
 * (dark photo canvas) — layout/content/composition order unchanged.
 *
 * Calendar entry point connected: [onViewCalendarClick] threads through
 * to [CalendarPreviewSection]'s "مشاهده تقویم کامل" CTA — wired in
 * [ai.rojan.designlab.manager.navigation.ManagerNavGraph], not hardcoded
 * here, so this screen stays navigation-agnostic (`{}` no-op default
 * still works standalone/in `@Preview`).
 */
@Composable
fun ManagerDashboardScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onViewCalendarClick: () -> Unit = {},
) {
    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceLG),
        ) {
            item { ManagerHeader() }
            item { SalonIdentityCard() }
            item { TodayOverviewSection() }
            item { QuickActionsSection() }
            item { AIInsightCard() }
            item { CalendarPreviewSection(onViewCalendarClick = onViewCalendarClick) }
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
