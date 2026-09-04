package ai.rojan.designlab.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color


// =====================================
// ROJAN AI - TEXT COLORS
// =====================================

val RojanTextPrimary =
    Color(0xFF4D355F)

val RojanTextSecondary =
    Color(0xFF867391)


// =====================================
// ROJAN AI - BRAND COLORS
// =====================================

/**
 * ROJAN AI Customer UI Final Visual Refinement addition: saturation
 * increased +5% via HSL math (hue/lightness unchanged) — 0x8E2DE2 to
 * 0x8E28E7. The other 3 accent tokens in this section
 * ([RojanVividMagenta], [RojanRose], [RojanAIGlow]) are already at
 * HSL's maximum saturation (s=1.0) and are mathematically unchanged by
 * a further "+5%" — not silently skipped, just genuinely already
 * maxed out.
 */
val RojanVividPurple =
    Color(0xFF8E28E7)

val RojanVividMagenta =
    Color(0xFFFF4FA3)

val RojanRose =
    Color(0xFFFF8FC8)

/**
 * Rojan.Brand.AIGlow (Design Token Spec v1.0, Section 2) — the "AI is
 * active" signal color, distinct from static brand purple. Promoted here
 * to a first-class brand token; previously this value only existed
 * buried inside RojanShadows as a shadow-glow color with no independent
 * identity of its own.
 */
val RojanAIGlow =
    Color(0xFF7C4DFF)

/**
 * Dark-canvas gradient mid-stop (Background System v1.0, Section 2) —
 * sits between [RojanNavy] and the brand purple family so the Gradient
 * Layer reads as one soft pool of depth rather than a hard jump straight
 * to full brand-purple saturation.
 */
val RojanDeepPurple =
    Color(0xFF2B1F45)

val RojanBlushPink =
    Color(0xFFFFC7DE)

val RojanPearlPink =
    Color(0xFFFFD6E8)

val RojanSoftLavender =
    Color(0xFFDCCBFF)

/**
 * ROJAN AI Customer UI Final Visual Refinement addition: saturation
 * increased +8% via HSL math (hue/lightness unchanged) — 0xB8F3EE to
 * 0xB6F5F0. The other 3 pastel tokens in this section
 * ([RojanBlushPink], [RojanPearlPink], [RojanSoftLavender]) are already
 * at HSL's maximum saturation (s=1.0) and are mathematically unchanged
 * by a further "+8%" — genuinely already maxed out, not skipped.
 */
val RojanAquaMint =
    Color(0xFFB6F5F0)

val RojanWarmWhite =
    Color(0xFFFFFBFF)

val RojanNavy =
    Color(0xFF18233A)


// =====================================
// ROJAN AI - BACKGROUND SYSTEM
// =====================================

val RojanBackgroundGradient =
    listOf(
        RojanWarmWhite,
        RojanBlushPink,
        RojanAquaMint,
        RojanSoftLavender,
        RojanWarmWhite
    )


// =====================================
// ROJAN AI - BUTTON SYSTEM
// =====================================

val RojanButtonGradient =
    listOf(
        RojanVividPurple,
        RojanVividMagenta
    )


val RojanButtonBrush =
    Brush.linearGradient(
        colors = RojanButtonGradient
    )


// =====================================
// ROJAN AI - HERO SYSTEM
// =====================================

val RojanHeroGradientColors =
    listOf(
        RojanVividPurple,
        RojanVividMagenta
    )


val RojanHeroGradient =
    Brush.linearGradient(
        colors = RojanHeroGradientColors
    )


// =====================================
// ROJAN AI - GLASS SYSTEM
// =====================================

val RojanGlassWhite =
    Color(0x66FFFFFF)

val RojanGlassBorder =
    Color(0x55FFFFFF)


// =====================================
// ROJAN AI - SHADOW SYSTEM
// =====================================

val RojanShadowPurple =
    Color(0x668E2DE2)

val RojanShadowPink =
    Color(0x66FF4FA3)


// =====================================
// ROJAN AI - EXTRA SYSTEM COLORS
// =====================================

val RojanButtonGradientStart =
    RojanVividPurple

val RojanButtonGradientEnd =
    RojanVividMagenta


// =====================================
// ROJAN AI - TEXT ON SURFACE (Design Token Spec v1.0, Section 2)
// New tokens — did not exist in code before Phase 3 Component Migration.
// Closes the gap flagged during HeroBookingCard's token evaluation: no
// existing token covered text sitting on a glass or dark surface.
// =====================================

/** Text sitting directly on a glass/gradient surface (e.g. Hero Card title). */
val RojanTextOnGlass =
    Color(0xFF4D355F)

/**
 * Text on a solid/gradient dark surface — a muted plum (`#6B5579`),
 * slightly softer than [RojanTextOnGlass].
 *
 * Doc-accuracy fix (UI Polish Sprint 1): an earlier "Luxury Typography &
 * Contrast Finalization" note claimed this was a near-white `#F4F6F8`;
 * the committed value was never white. The Customer app has since moved
 * to its own dark palette ([ai.rojan.designlab.screens.customer.hometheme.HomeColors]),
 * so this token is now read only by a few legacy surfaces
 * ([ai.rojan.designlab.screens.dashboard.DashboardPlaceholder], and a
 * doc reference in [ai.rojan.designlab.components.hero.HeroBookingCard]).
 * Value left unchanged — this pass only corrects the description.
 */
val RojanTextOnDarkSurface =
    Color(0xFF6B5579)


// =====================================
// ROJAN AI - BOOKING CATEGORY ACCENTS
// (Booking Module Design System Migration)
// These preserve genuinely distinct, deliberately-varied per-category
// colors found in screens/booking/ that don't match any existing token
// (unlike the module's dark/secondary text and its repeated #8B5CF6
// accent, which were close enough to RojanTextPrimary/Secondary/AIGlow
// to reuse those directly instead of adding near-duplicates here).
// =====================================

val RojanCategorySkinStart = Color(0xFFCFFAFE)
val RojanCategorySkinEnd = Color(0xFF22D3EE)
val RojanCategorySkinIcon = Color(0xFF06B6D4)

val RojanCategoryNailsStart = Color(0xFFFBCFE8)
val RojanCategoryNailsEnd = Color(0xFFEC4899)
val RojanCategoryNailsIcon = Color(0xFFEC4899)

val RojanCategoryMakeupStart = Color(0xFFFFEDD5)
val RojanCategoryMakeupEnd = Color(0xFFF97316)
val RojanCategoryMakeupIcon = Color(0xFFF97316)

val RojanCategoryHairStart = Color(0xFFE9D5FF)
val RojanCategoryHairEnd = Color(0xFF8B5CF6)

val RojanAvatarGradientStart = Color(0xFFF9A8D4)

/** Star-rating gold — distinct from any existing token, genuinely new semantic color. */
val RojanRatingGold = Color(0xFFFFB020)

/** "Online now" status green — same reasoning as [RojanRatingGold]. */
val RojanStatusOnline = Color(0xFF16A34A)

// =====================================
// ROJAN AI - SEMANTIC TEXT TOKENS (Theme & Typography Architecture Normalization)
// Every token below is an ALIAS onto an already-approved color above —
// none of these are new colors. This is the single source of truth for
// "which named text role uses which color" so call sites reference a
// role (e.g. RojanSemanticColors.ErrorText), not a raw Color value.
// =====================================

/**
 * Theme & Typography Architecture Normalization: promotes the error-red
 * that already existed hardcoded inline (`Color(0xFFFF5C7A)` in
 * [ai.rojan.designlab.screens.auth.AuthScreen]) to a proper token — this
 * color was already part of the app's real rendered UI, not a new
 * invention; this just gives it a name so every future error message
 * uses the same value instead of each screen hardcoding it separately.
 */
val RojanErrorText = Color(0xFFFF5C7A)

/** Success/confirmation text — reuses [RojanStatusOnline], the same green already used for "online now" status, rather than inventing a second green. */
val RojanSuccessText = RojanStatusOnline

/** Warning text — reuses [RojanRatingGold] (an existing warm amber/gold tone) rather than inventing a new warning color. */
val RojanWarningText = RojanRatingGold

/** Muted/de-emphasized text (e.g. optional metadata) — [RojanTextSecondary] at reduced opacity. Distinct from body text (never reduced) - this is specifically for content that's semantically meant to recede. */
val RojanMutedText = Color(0xFF9D8AA7)

/** Hint/placeholder text — more muted than [RojanMutedText], for empty-state prompts (e.g. search bar placeholder). */
val RojanHintText = Color(0xFFB5A9BE)

/** Disabled/inactive control text — most muted of the de-emphasized tier. */
val RojanDisabledText = Color(0xFFC6BECF)

/** Text on a glass surface — alias onto the existing [RojanTextOnGlass]. */
val RojanGlassText = RojanTextOnGlass

/**
 * Text on a solid dark surface — the brand plum `#4D355F` (the same value
 * as [RojanTextPrimary]/[RojanTextOnGlass], not the near-white an earlier
 * doc comment claimed). Doc-accuracy fix (UI Polish Sprint 1) — value
 * unchanged. Legacy: read only by [ai.rojan.designlab.ui.components.brand.VersionFooter]
 * and [ai.rojan.designlab.components.AIHeader]; new dark-surface text uses
 * [ai.rojan.designlab.screens.customer.hometheme.HomeColors] /
 * `ManagerColors`.
 */
val RojanDarkSurfaceText = Color(0xFF4D355F)

/** Text on a light/white surface (glass cards, dialogs) — alias onto the existing [RojanTextPrimary]. */
val RojanLightSurfaceText = RojanTextPrimary

/** Button label text — white, matching [RojanTextOnGlass] since buttons use the same brand-gradient fill as glass hero surfaces. */
val RojanButtonText = Color(0xFFFFFFFF)

/** Card title — alias onto [RojanTextPrimary]. */
val RojanCardTitle = RojanTextPrimary

/** Card subtitle/secondary line — alias onto [RojanTextSecondary]. */
val RojanCardSubtitle = RojanTextSecondary

/** Bottom navigation / tab labels — alias onto [RojanTextSecondary] (the existing inactive-tab color; the active-tab color is [RojanAIGlow], unchanged, applied separately per-component). */
val RojanNavigationLabel = RojanTextSecondary

/** Section headings on the dark canvas (e.g. "تایید رزرو") — alias onto [RojanTextOnGlass], matching existing HeroTitle usage throughout the Booking Journey. */
val RojanSectionTitle = RojanTextOnGlass

/** Small secondary/meta text — alias onto [RojanTextSecondary], matching [RojanTypography.Caption]'s existing real-world usage. */
val RojanCaptionText = RojanTextSecondary

// =====================================
// ROJAN AI - LUXURY TYPOGRAPHY (LEGACY)
// The seven unused members of this family (PrimaryHeading / SectionHeading
// / HeroTitle / PrimaryBody / SecondaryBody / Hint / Disabled) were removed
// in the design-system cleanup pass — the Customer and Manager apps each
// ship their own dark-surface text palette (HomeColors / ManagerColors),
// and those tokens had zero call sites.
//
// Only RojanLuxuryCaption survives: its single caller is in the navigation
// package, which is out of scope for this pass. Migrate that one call site
// to [RojanCaptionText] / a palette caption color, then this token can go
// too.
// =====================================

/**
 * Legacy — muted mauve `#9D8AA7`. Single remaining caller: `RojanNavGraph`.
 * Pending migration to [RojanCaptionText]; do not add new call sites.
 */
val RojanLuxuryCaption = Color(0xFF9D8AA7)

// =====================================
// ROJAN AI - LOADING SYSTEM
// =====================================

/** [PremiumLoadingBar] track — faint translucent white. */
val RojanLoadingTrack = Color(0x22FFFFFF)

/** [PremiumLoadingBar] sweeping glow gradient — warm amber, distinct from [RojanRatingGold]. */
val RojanLoadingGlowStart = Color(0xFFFFE8C7)
val RojanLoadingGlowMid = Color(0xFFF6C47C)
val RojanLoadingGlowEnd = Color(0xFFE9A857)

// =====================================
// ROJAN AI - PREMIUM GLASS BORDER
// Shared Rose Gold + Gold metallic border language (Premium Glass Border
// Design Update), applied consistently across every glass surface in
// every module via the Shared Premium Glass Design System's
// PremiumGlassSurface (GlassSurface/HomeGlassSurface/ManagerGlassSurface
// are thin wrappers around it) — per
// design/reference/Premium_Glass_Border_Reference.png.
// Border-only tokens: fill, layout, typography, and shadows are untouched.
// =====================================
val RojanPremiumBorderRoseGold = Color(0xFFE0A67A)
val RojanPremiumBorderGold = Color(0xFFD4AF37)

/** Bright warm-champagne specular stop — simulates polished metal catching light at one point along the edge, not a flat single-tone line. */
val RojanPremiumBorderHighlight = Color(0xFFFCE7B8)

/** Darkened-gold contrast band — real polished metal reads as metal because it alternates bright and shadowed passages, not one flat mid-tone. */
val RojanPremiumBorderShadow = Color(0xFF7A4A22)

/** Near-white warm-gold specular — the sharpest glint tone, distinct from [RojanPremiumBorderHighlight]'s broader champagne band; reserved for corner sparkle points and tight reflection lines. */
val RojanPremiumBorderSpecular = Color(0xFFFFF6E0)

// =====================================
// ROJAN AI - DECORATIVE ORB TOKENS
// Design-system refinement, Phase 4 — semantic aliases for
// ai.rojan.designlab.components.GlassOrb's own @Preview functions (the
// only place this file had raw hex: GlassOrb lives in the flavor-agnostic
// components/ package, so it can't import a flavor's own theme file
// (HomeColors/ManagerColors) the way a real call site would — these alias
// the closest existing approved brand tones instead of inventing new hex,
// same convention as every other alias in this file.
// =====================================

/** Alias onto [RojanSoftLavender] — was `Color(0xFFD1B3FF)`, an unnamed near-duplicate of this exact tone. */
val RojanOrbLavenderGlow = RojanSoftLavender

/** Alias onto [RojanBlushPink] — was `Color(0xFFFFC1D6)`, an unnamed near-duplicate of this exact tone. */
val RojanOrbBlushGlow = RojanBlushPink
