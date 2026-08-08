package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.components.state.RojanComingSoonState
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Customer Home "Popular Services" section.
 *
 * Production Data Integrity Phase 1: gated. This section listed services
 * across every salon, but the real `ServiceApi` is salon-scoped only
 * (`GET /salons/{id}/categories/{categoryId}/services` — confirmed by
 * reading `data/remote/ServiceApi.kt` directly); no cross-salon "all
 * services" endpoint exists. Showing it would require either an N+1 loop
 * over every browsed salon (ruled out) or picking one arbitrary salon to
 * stand in for "popular" (ruled out — silently changes what the section
 * means). Was previously backed by
 * [ai.rojan.designlab.domain.catalog.CatalogEngine.allServices] (demo).
 * Entry point/section stays on Customer Home; content becomes a Coming
 * Soon state. **Backend follow-up required:** a dedicated cross-salon
 * aggregate endpoint (e.g. "popular services") — not implemented in this
 * Android-only phase.
 */
@Composable
fun PopularServices() {
    Column(
        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceTitleToContent),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "خدمات محبوب",
                style = RojanTypography.CardTitle,
                color = HomeColors.TextPrimary,
            )
        }

        RojanComingSoonState(modifier = Modifier.fillMaxWidth())
    }
}
