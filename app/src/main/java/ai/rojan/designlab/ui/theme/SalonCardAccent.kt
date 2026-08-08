package ai.rojan.designlab.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Deterministic per-salon card tint. The backend has no per-salon color/
 * branding concept, so this only varies a card's decorative accent from a
 * small fixed palette — it never fabricates business data (rating,
 * distance, etc.). Shared by every screen that renders a real
 * [ai.rojan.designlab.domain.repository.Salon] as a card
 * (`SalonListScreen`, `FeaturedSalons`, `RecommendedSalons`).
 */
private val salonCardPalette = listOf(RojanSoftLavender, RojanAquaMint, RojanBlushPink, RojanPearlPink)

fun salonAccentColorFor(salonId: String): Color = salonCardPalette[Math.floorMod(salonId.hashCode(), salonCardPalette.size)]
