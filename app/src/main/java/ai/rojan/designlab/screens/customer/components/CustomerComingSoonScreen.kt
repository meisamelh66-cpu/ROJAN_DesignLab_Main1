package ai.rojan.designlab.screens.customer.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/* =============================================================================
 * ROJAN Customer — the shared "coming soon" screen.
 *
 * UI Standardization Phase 3. Replaces seven byte-identical hand-rolled
 * placeholder screens (Wallet, Coupons, Membership, Loyalty, My Reviews,
 * Waitlist, Beauty Timeline) — each a `HomeBackgroundTheme { LazyColumn {
 * GlassBackButton; Text(HeroTitle); RojanComingSoonState() } }` — with one
 * quiet-luxury screen: `CustomerScaffold` chrome (flat 56dp bar, RTL back
 * arrow, 1px hairline) wrapping the flat `CustomerEmptyState`.
 *
 * These routes stay reachable from Profile so the customer sees the feature
 * is planned; the content is this until a real backend capability exists
 * (Production Data Integrity Phase 1 — "no mock data in production flows").
 *
 * Adds NO design-system token and edits NO shared component. Touches no
 * ViewModel, repository, navigation route, or API.
 * ========================================================================== */

@Composable
fun CustomerComingSoonScreen(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    body: String = "این بخش به‌زودی با اطلاعات واقعی فعال می‌شود",
    icon: ImageVector = Icons.Outlined.Schedule,
) {
    CustomerScaffold(
        title = title,
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        CustomerEmptyState(
            title = "به‌زودی",
            body = body,
            icon = icon,
        )
    }
}
