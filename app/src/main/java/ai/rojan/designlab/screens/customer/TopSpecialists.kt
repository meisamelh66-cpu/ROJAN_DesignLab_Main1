package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import ai.rojan.designlab.ui.components.state.RojanComingSoonState

/**
 * Customer Home top specialists.
 *
 * Production Data Integrity Phase 1: gated. This section listed
 * specialists across every salon, but the real `SpecialistApi` is
 * salon-scoped only (`GET /salons/{id}/specialists` — confirmed by
 * reading `data/remote/SpecialistApi.kt` directly); no cross-salon "all
 * specialists" endpoint exists. Showing it would require either an N+1
 * loop over every browsed salon (ruled out) or picking one arbitrary
 * salon to stand in for "top" (ruled out — silently changes what the
 * section means). Was previously backed by
 * [ai.rojan.designlab.domain.catalog.CatalogEngine.allSpecialists] (demo,
 * including a fabricated `rating`/`reviewCount` — no such fields exist on
 * the real `Specialist`). Entry point/section stays wherever this is
 * composed (Customer Home, Customer Dashboard); content becomes a Coming
 * Soon state. **Backend follow-up required:** a dedicated cross-salon
 * aggregate endpoint (e.g. "top specialists") — not implemented in this
 * Android-only phase.
 */
@Composable
fun TopSpecialists(onSpecialistClick: (String) -> Unit = {}) {
    RojanComingSoonState(modifier = Modifier.fillMaxWidth())
}
