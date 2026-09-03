package ai.rojan.designlab.ui.components.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign

import ai.rojan.designlab.ui.components.glass.PremiumGlassSurface
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.motion.RojanAiThinkingIndicator
import ai.rojan.designlab.ui.motion.rememberReducedMotion
import ai.rojan.designlab.ui.motion.rojanAiGlow
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.LocalRojanPalette
import ai.rojan.designlab.ui.theme.RojanAppPalette
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * The lifecycle of an AI-driven surface (UI Polish Sprint 4).
 *
 * These are the ONLY states an AI surface may be in. There is no "show a
 * plausible-looking placeholder" state by design — see
 * `docs/uiux/ROJAN_AI_UI_GOVERNANCE_v1.md`.
 */
enum class RojanAIState {
    /** Real content is available and passed via the surface's `content` slot. */
    Available,

    /** A real request is in flight. Shows the calm thinking indicator. */
    Loading,

    /** The request completed and there is genuinely nothing to surface right now. Honest "nothing" copy, never invented content. */
    Empty,

    /** The request failed. Honest error copy + optional retry. */
    Error,

    /** The capability is switched off / not provisioned for this account. Muted, no glow. */
    Disabled,
}

/**
 * ROJAN AI Surface — the shared "this surface is intelligent" shell
 * (UI Polish Sprint 3 introduced it; Sprint 4 gave it a full state
 * machine).
 *
 * **Foundation only — zero AI logic.** It renders the premium AI identity
 * and the five [RojanAIState]s; the caller decides which state applies
 * from real data, and supplies real content for [RojanAIState.Available]
 * via [content]. This file never fabricates a title, a number, a
 * recommendation, or a "confidence".
 *
 * Identity:
 * - the frozen [PremiumGlassSurface] mechanic — an AI surface is
 *   unmistakably ROJAN glass, not a parallel look;
 * - a slow, low-alpha brand glow ([rojanAiGlow], from Sprint 2) — only in
 *   [RojanAIState.Available] / [RojanAIState.Loading], the states where
 *   "AI is working / has something". Empty/Error/Disabled do not glow;
 * - a small "AI" chip in the trailing-top position (reads first in RTL);
 * - palette-driven accent (`LocalRojanPalette.textAccent`) — Gold for
 *   Manager, AI-purple for Customer;
 * - calm motion: reduced-motion holds the glow still and the thinking
 *   indicator static (both handled by the Sprint 2 primitives).
 */
@Composable
fun RojanAISurface(
    modifier: Modifier = Modifier,
    state: RojanAIState = RojanAIState.Available,
    accentColor: Color = LocalRojanPalette.current.textAccent,
    showBadge: Boolean = true,
    badgeLabel: String = "هوش مصنوعی",
    shape: Shape = RojanShapes.GlassCard,
    contentPadding: PaddingValues = PaddingValues(RojanDimens.SpaceMD),
    // Honest, caller-supplied copy for the non-content states. Defaults are
    // deliberately generic ("nothing right now" / "couldn't load") — they
    // state a fact about the request, never imply content.
    loadingLabel: String = "در حال بررسی…",
    emptyTitle: String = "در حال حاضر نکته‌ای برای نمایش نیست",
    emptyDescription: String? = null,
    errorTitle: String = "دریافت اطلاعات ممکن نشد",
    errorDescription: String? = null,
    disabledLabel: String = "این قابلیت فعال نیست",
    onRetry: (() -> Unit)? = null,
    retryLabel: String = "تلاش مجدد",
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalRojanPalette.current
    val reduceMotion = rememberReducedMotion()
    val glowing = state == RojanAIState.Available || state == RojanAIState.Loading

    Box(
        modifier = modifier
            .alpha(if (state == RojanAIState.Disabled) 0.5f else 1f)
            .then(
                if (glowing) {
                    Modifier.rojanAiGlow(color = accentColor, reduceMotion = reduceMotion)
                } else {
                    Modifier
                },
            ),
    ) {
        PremiumGlassSurface(modifier = Modifier.fillMaxWidth(), shape = shape) {
            Column(modifier = Modifier.fillMaxWidth().padding(contentPadding)) {
                if (showBadge) {
                    RojanAIBadge(
                        accentColor = accentColor,
                        label = badgeLabel,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(bottom = RojanDimens.SpaceSM),
                    )
                }

                when (state) {
                    RojanAIState.Available -> content()

                    RojanAIState.Loading -> Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = RojanDimens.SpaceSM),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
                    ) {
                        RojanAiThinkingIndicator(color = accentColor, reduceMotion = reduceMotion)
                        Text(text = loadingLabel, style = RojanTypography.Body, color = palette.textSecondary)
                    }

                    RojanAIState.Empty -> RojanAIStateMessage(
                        icon = Icons.Filled.Lightbulb,
                        iconTint = palette.textSecondary,
                        title = emptyTitle,
                        description = emptyDescription,
                        palette = palette,
                    )

                    RojanAIState.Error -> {
                        RojanAIStateMessage(
                            icon = Icons.Filled.CloudOff,
                            iconTint = palette.textSecondary,
                            title = errorTitle,
                            description = errorDescription,
                            palette = palette,
                        )
                        if (onRetry != null) {
                            Spacer(Modifier.height(RojanDimens.SpaceSM))
                            Text(
                                text = retryLabel,
                                style = RojanTypography.Button,
                                color = accentColor,
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .rojanPressable(onClick = onRetry)
                                    .padding(RojanDimens.SpaceSM),
                            )
                        }
                    }

                    RojanAIState.Disabled -> Text(
                        text = disabledLabel,
                        style = RojanTypography.Body,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(vertical = RojanDimens.SpaceSM),
                    )
                }
            }
        }
    }
}

@Composable
private fun RojanAIStateMessage(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String?,
    palette: RojanAppPalette,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = RojanDimens.SpaceSM),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        RojanIconContainer(imageVector = icon, contentDescription = null, size = RojanIconSize.Large, tint = iconTint)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = title, style = RojanTypography.Body, color = palette.textPrimary)
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = RojanTypography.Caption,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(top = RojanDimens.SpaceXS),
                )
            }
        }
    }
}

/**
 * The small "AI" pill used by [RojanAISurface] (reusable standalone — e.g.
 * on an "AI recommended" section header). Sparkle glyph + short label,
 * tinted with the supplied accent on a faint accent wash.
 */
@Composable
fun RojanAIBadge(
    accentColor: Color,
    modifier: Modifier = Modifier,
    label: String = "هوش مصنوعی",
) {
    Row(
        modifier = modifier
            .background(accentColor.copy(alpha = 0.14f), RojanShapes.Circle)
            .padding(horizontal = RojanDimens.SpaceSM, vertical = RojanDimens.SpaceXS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
    ) {
        Text(text = label, style = RojanTypography.Caption, color = accentColor, textAlign = TextAlign.Center)
        Box(modifier = Modifier.size(RojanDimens.IconSizeSmall)) {
            RojanIconContainer(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                size = RojanIconSize.Small,
                tint = accentColor,
            )
        }
    }
}
