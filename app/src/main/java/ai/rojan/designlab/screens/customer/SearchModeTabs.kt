package ai.rojan.designlab.screens.customer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign

import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanRose
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography

/** Fake, local-only search-mode identifiers — Phase 2 scope is the toggle control itself, not real search/filtering. */
enum class SearchMode { SERVICES, SALONS }

/**
 * Smart Search area, Phase 2 — the "خدمات" / "سالن‌ها" mode toggle
 * directly below [AISearchBar]. Purely a demo-state UI control per this
 * phase's explicit scope ("Do NOT implement real search... Use demo
 * state only") — [selected]/[onSelectedChange] are hoisted to
 * [CustomerHomeScreen] rather than held locally in this composable, so
 * the choice survives this item scrolling out of and back into a
 * [androidx.compose.foundation.lazy.LazyColumn]'s composition window
 * (a plain `remember` inside this composable would not).
 *
 * Same [GlassSurface] + pill shape ([RojanShapes.PremiumButton]) language
 * as every other premium control on this screen; the selected segment's
 * fill/text colors cross-fade via [animateColorAsState] between
 * [RojanTextSecondary]/transparent (inactive) and [RojanTextPrimary]/
 * [RojanRose] (active). Home Premium Polish Phase: the selected pill was
 * previously a solid, fully-opaque [RojanAIGlow] (vivid purple) — swapped
 * for a soft, translucent [RojanRose] tint (glass still shows through)
 * to match the approved reference's softer rose/pink selection style,
 * with dark [RojanTextPrimary] label text (readable against a light
 * pastel fill, unlike the white label the previous solid-purple fill
 * needed).
 */
@Composable
fun SearchModeTabs(
    selected: SearchMode,
    onSelectedChange: (SearchMode) -> Unit,
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RojanShapes.PremiumButton,
        borderAlpha = 0.10f,
        borderSecondaryAlpha = 0.04f,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceXS),
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
        ) {
            SearchModeTab(
                label = "خدمات",
                isSelected = selected == SearchMode.SERVICES,
                onClick = { onSelectedChange(SearchMode.SERVICES) },
                modifier = Modifier.weight(1f),
            )
            SearchModeTab(
                label = "سالن‌ها",
                isSelected = selected == SearchMode.SALONS,
                onClick = { onSelectedChange(SearchMode.SALONS) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SearchModeTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    // animateColorAsState needs an AnimationSpec<Color>, not the shared
    // AnimationSpec<Float> RojanAnimations.ContentEnterSpec is typed as —
    // same duration/easing values, just re-typed for Color here.
    val colorAnimationSpec = tween<androidx.compose.ui.graphics.Color>(
        durationMillis = 420,
        easing = FastOutSlowInEasing,
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) RojanRose.copy(alpha = 0.45f) else RojanRose.copy(alpha = 0f),
        animationSpec = colorAnimationSpec,
        label = "search_mode_tab_container",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) RojanTextPrimary else RojanTextSecondary,
        animationSpec = colorAnimationSpec,
        label = "search_mode_tab_content",
    )

    Row(
        modifier = modifier
            .heightIn(min = RojanDimens.MinTouchTarget)
            .clip(RojanShapes.PremiumButton)
            .background(containerColor, RojanShapes.PremiumButton)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = RojanTypography.Body,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
    }
}
