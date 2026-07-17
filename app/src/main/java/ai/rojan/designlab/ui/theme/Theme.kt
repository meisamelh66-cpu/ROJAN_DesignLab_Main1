package ai.rojan.designlab.ui.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Theme & Typography Architecture Normalization. This was previously
 * `MaterialTheme(content = content)` — zero ColorScheme, zero
 * Typography, zero Shapes passed in, meaning `MaterialTheme.colorScheme`/
 * `.typography`/`.shapes` resolved to Compose's raw defaults everywhere,
 * completely disconnected from [RojanTokens]. Confirmed via the
 * pre-migration audit that no component actually reads
 * `MaterialTheme.colorScheme`/`.typography` today (everything reads
 * [RojanTokens]/[RojanTypography]/[RojanShapes] directly) — so this
 * migration is genuinely additive: it gives Material3's own theming
 * system a real foundation for the first time, without changing what
 * any existing component currently renders, since nothing was reading
 * from `MaterialTheme.*` before to begin with.
 *
 * Every value below maps to an existing [RojanTokens] color — none are
 * new. Where Material3 wants a role [RojanTokens] never named
 * explicitly (e.g. `outline`, `scrim`), the closest existing approved
 * color is reused rather than a new one invented; each choice is
 * commented with its reasoning below.
 */
private val RojanColorScheme = lightColorScheme(
    // Global Typography & Theme Refactor: the app's real canvas is now
    // the Light Luxury photographic background (bright pastel), not
    // dark — background/onBackground/onSurface reflect that. Saturated
    // fills (primary/secondary/tertiary/error) still need [RojanButtonText]
    // (white) for their "on" pairing, since white-on-saturated-color
    // contrast is unaffected by the canvas brightness change — only
    // text sitting on the light canvas/glass itself needed to flip
    // from white to dark ([RojanTextOnGlass] et al, in RojanTokens.kt).
    primary = RojanVividPurple,
    onPrimary = RojanButtonText,
    primaryContainer = RojanSoftLavender,
    onPrimaryContainer = RojanTextPrimary,

    secondary = RojanVividMagenta,
    onSecondary = RojanButtonText,
    secondaryContainer = RojanPearlPink,
    onSecondaryContainer = RojanTextPrimary,

    tertiary = RojanAIGlow,
    onTertiary = RojanButtonText,

    background = RojanWarmWhite,
    onBackground = RojanTextOnGlass,

    // Surface = the light/glass-card family (RojanWarmWhite base), since
    // that's what GlassSurface/dialogs/cards actually render as —
    // conceptually the same tier as background now, both light.
    surface = RojanWarmWhite,
    onSurface = RojanTextPrimary,
    surfaceVariant = RojanSoftLavender,
    onSurfaceVariant = RojanTextSecondary,
    surfaceTint = RojanVividPurple,

    // No dedicated "outline" token exists in RojanTokens - reusing
    // RojanTextSecondary at reduced alpha (a muted neutral already
    // established for de-emphasized content) rather than inventing a
    // new gray.
    outline = RojanTextSecondary.copy(alpha = 0.5f),
    outlineVariant = RojanTextSecondary.copy(alpha = 0.25f),

    error = RojanErrorText,
    onError = RojanButtonText,
    errorContainer = RojanErrorText.copy(alpha = 0.16f),
    onErrorContainer = RojanErrorText,

    // inverseSurface stays dark (Navy) - it's the deliberate exception
    // surface, so its "on" text correctly stays white (RojanButtonText),
    // unlike the now-light primary surface/background above.
    inverseSurface = RojanNavy,
    inverseOnSurface = RojanButtonText,
    inversePrimary = RojanSoftLavender,

    // No dedicated scrim token exists - RojanNavy (the darkest existing
    // approved color) is the closest reasonable choice; Material3
    // applies its own alpha to this value for modal barriers.
    scrim = RojanNavy,
)

/**
 * Material3 [Typography] built from [RojanTypography]'s real values —
 * not a second, separate typography definition. [RojanTypography]
 * remains the single source; this only maps its 4 existing levels onto
 * the Material3 slots closest in intent, and fills the remaining slots
 * with [RojanTypography.Body] as a safe default (rather than Compose's
 * generic defaults) so anything that *does* read `MaterialTheme.typography`
 * in the future gets ROJAN's font sizing, not Material's.
 */
private val RojanMaterialTypography = Typography(
    headlineLarge = RojanTypography.HeroTitle,
    headlineMedium = RojanTypography.HeroTitle,
    titleLarge = RojanTypography.HeroTitle,
    titleMedium = RojanTypography.Body.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = RojanTypography.Body.copy(fontWeight = FontWeight.Medium),
    bodyLarge = RojanTypography.Body,
    bodyMedium = RojanTypography.Body,
    bodySmall = RojanTypography.Caption,
    labelLarge = RojanTypography.Button,
    labelMedium = RojanTypography.Button,
    labelSmall = RojanTypography.Caption,
)

/**
 * Material3 [Shapes] built from [RojanShapes]'s real values. Material3
 * shapes are specified as [CornerSize]s per corner-radius tier, not
 * arbitrary per-corner shapes like [RojanShapes.GlassCard] supports —
 * so this maps each tier to the closest matching [RojanShapes] radius
 * value rather than trying to reproduce every custom corner
 * configuration exactly (components that need [RojanShapes.GlassCard]'s
 * exact 4-corner definition keep reading it directly, unchanged; this
 * is only for the few call sites that might read `MaterialTheme.shapes`).
 */
private val RojanMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(CornerSize(8)),
    small = RoundedCornerShape(CornerSize(16)), // matches RojanShapes.Small
    medium = RoundedCornerShape(CornerSize(24)),
    large = RoundedCornerShape(CornerSize(32)), // matches RojanShapes.GlassCard's radius
    extraLarge = RoundedCornerShape(CornerSize(50)), // matches RojanShapes.PremiumButton
)

@Composable
fun RojanTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RojanColorScheme,
        typography = RojanMaterialTypography,
        shapes = RojanMaterialShapes,
        content = content,
    )
}
