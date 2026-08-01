package ai.rojan.designlab.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object RojanDimens {

    // Spacing
    val SpaceXS = 4.dp
    val SpaceSM = 8.dp
    val SpaceMD = 16.dp
    val SpaceLG = 24.dp
    val SpaceXL = 32.dp
    val SpaceXXL = 48.dp

    /**
     * Shared Premium Glass Design System — dashboard rhythm tokens, one
     * name per semantic role rather than reusing the generic scale
     * tokens directly at each call site. Every app (Manager, Customer,
     * and future Specialist/Reception/etc.) renders its stacked-card
     * dashboard lists off these three, never a raw dp literal or a bare
     * [SpaceXS]/[SpaceMD] chosen ad hoc per screen — so the whole rhythm
     * moves from one place if it's ever retuned again. Values match what
     * was already verified on-device as the compact/"one connected
     * dashboard" feel (see CLAUDE.md's "Spacing rhythm" section); this
     * only names them, it doesn't change them. Scope carve-out (see
     * CLAUDE.md): applies to stacked-card dashboard-style screens, not
     * booking-flow steps or single-entity detail screens — those keep
     * whatever spacing already suits their own different layout shape.
     */
    val SpaceTitleToContent = SpaceXS
    val SpaceCardToCard = SpaceMD
    val SpaceSectionToSection = SpaceMD


    // Corner Radius
    val RadiusSM = 12.dp
    val RadiusMD = 20.dp
    val RadiusLG = 32.dp
    val RadiusXL = 48.dp


    // Glass
    val GlassBorder = 1.dp


    // Button
    val ButtonHeight = 64.dp
    val ButtonWidth = 240.dp


    // Hero
    val HeroHeight = 360.dp


    // Typography
    val TitleSize = 32.sp
    val SubtitleSize = 18.sp
    val BodySize = 15.sp


    /**
     * Design System Refinement Phase 2 — Icon System. Consolidated from
     * a real audit of every `Modifier.size()` call on an `Icon(...)`
     * project-wide: 16 distinct raw dp values were in use (14/16/18/20/
     * 22/24/28/30/32/40/44/45/48/56/100/160dp), scattered with no token
     * behind any of them. These 4 tiers were chosen as the values
     * closest to the ACTUAL most-common sizes found (14dp: 19 uses -
     * inline meta/rating icons; ~20-24dp cluster: standard row icons;
     * ~32dp: prominent card icons; ~48-56dp: avatar-adjacent feature
     * icons) rather than arbitrary round numbers, so migrating existing
     * call sites onto these tokens causes no visually jarring size
     * jumps.
     */
    val IconSizeSmall = 14.dp
    val IconSizeMedium = 20.dp
    val IconSizeLarge = 32.dp
    val IconSizeXLarge = 56.dp

    /**
     * Design System Phase 2.5 spacing audit: the two highest-frequency
     * repeated raw dp values found project-wide (8 and 6 occurrences
     * respectively) that had no token behind them - the standard size
     * for horizontal-scroll cards (FeaturedSalons/TopSpecialists/
     * NearbySalons/RecommendedServices/UpcomingBookings/RecentVisits/
     * FavoriteSalons all independently hardcoded this same 160x190
     * pair). [BackButtonSize] likewise consolidates [GlassBackButton]'s
     * repeated 44dp.
     */
    val CardWidthStandard = 160.dp
    val CardHeightStandard = 190.dp

    /**
     * UI Consolidation Sprint v2.0, item 10 (Accessibility): "Minimum
     * touch size: 48dp. No tiny clickable areas." Previously 44dp -
     * below that minimum. Renamed conceptually to a general minimum
     * touch target, still used for [GlassBackButton]'s own size.
     */
    val BackButtonSize = 48.dp
    val MinTouchTarget = 48.dp
}