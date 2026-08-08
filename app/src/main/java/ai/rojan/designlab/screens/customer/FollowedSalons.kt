package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import ai.rojan.designlab.ui.components.state.RojanComingSoonState

/**
 * Customer Home "Followed Salons".
 *
 * Production Data Integrity Phase 1: gated. The follow/favorite toggle
 * this section listed (`CustomerEcosystemViewModel.state.favoriteSalonIds`)
 * has no backend persistence anywhere — no favorite/follow endpoint exists
 * on `SalonApi`, and the underlying state was in-memory, per-session only,
 * literally defaulting from `DemoSalonRepository.salons.map { it.id }`.
 * Migrating just the salon *read* while keeping that fake toggle would
 * still be simulating a feature the backend doesn't support. Entry
 * point/section stays on Customer Home; content becomes a Coming Soon
 * state until the backend adds real favorite/follow persistence.
 */
@Composable
fun FollowedSalons(onSalonClick: (String) -> Unit = {}) {
    RojanComingSoonState(modifier = Modifier.fillMaxWidth())
}
