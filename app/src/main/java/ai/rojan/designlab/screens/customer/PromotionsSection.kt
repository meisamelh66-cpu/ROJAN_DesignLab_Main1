package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import ai.rojan.designlab.ui.components.state.RojanComingSoonState

/**
 * Customer Home promotions.
 *
 * Production Data Integrity Phase 1: gated. No promotions endpoint exists
 * anywhere in `data/remote/dto/` or `domain/repository/` — this was
 * previously backed entirely by
 * [ai.rojan.designlab.domain.catalog.CatalogEngine.promotions] (demo-only,
 * zero backend counterpart). Entry point/section stays on Customer Home;
 * content becomes a Coming Soon state until a real promotions capability
 * exists on the backend.
 */
@Composable
fun PromotionsSection() {
    RojanComingSoonState(modifier = Modifier.fillMaxWidth())
}
