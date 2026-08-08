package ai.rojan.designlab.screens.customer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import ai.rojan.designlab.ui.components.state.RojanComingSoonState

/**
 * Customer Home nearby salons.
 *
 * Production Data Integrity Phase 1: gated. The backend has no
 * geolocation/distance concept (`Salon` — see `domain/repository/SalonRepository.kt`
 * — carries no coordinates), so "nearby" cannot be computed honestly —
 * showing salons without real distance would just duplicate
 * [FeaturedSalons] under a false label. Was previously backed by
 * [ai.rojan.designlab.domain.catalog.CatalogEngine]/`DemoSalon.distanceKm`
 * (a fabricated value). Entry point/section stays on Customer Home; content
 * becomes a Coming Soon state until real location support exists.
 */
@Composable
fun NearbySalons(onSalonClick: ((String) -> Unit)? = null) {
    RojanComingSoonState(modifier = Modifier.fillMaxWidth())
}
