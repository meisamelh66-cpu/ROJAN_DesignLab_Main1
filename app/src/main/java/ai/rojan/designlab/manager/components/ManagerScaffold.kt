package ai.rojan.designlab.manager.components

import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Manager App workspace screen wrapper — same structure as
 * [ai.rojan.designlab.ui.components.scaffold.RojanScaffold] (left
 * untouched; still used by Customer/legacy screens), but built on
 * [ManagerBackgroundTheme] (dark luxury glass) instead of the shared
 * `WarmBackground`/`PremiumBackground`.
 *
 * ROJAN AI Manager Visual Theme Implementation: swapped from
 * `WarmBackground` to [ManagerBackgroundTheme] per the approved
 * reference (`design/reference/ROJAN_Manager_Reference.png`) — presentation
 * only, this wrapper's structure/parameters are unchanged. The shared
 * `GlassBackButton` (white glass + dark-purple icon, correct for
 * Customer's light theme) would read as a mismatched pale smudge on
 * this dark background, so this scaffold now has its own
 * [ManagerBackButton] using [ManagerGlassSurface] instead — the shared
 * button and its ~24 Customer call sites are untouched.
 */
@Composable
fun ManagerScaffold(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(RojanDimens.SpaceMD),
    content: @Composable () -> Unit,
) {
    ManagerBackgroundTheme {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                content()
            }

            if (onBackClick != null) {
                ManagerBackButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(RojanDimens.SpaceLG),
                )
            }
        }
    }
}

/** Manager-only counterpart to the shared `GlassBackButton`, re-themed for the dark luxury glass background. */
@Composable
private fun ManagerBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ManagerGlassSurface(
        modifier = modifier
            .size(RojanDimens.BackButtonSize)
            .rojanPressable(onClick = onClick),
        shape = RojanShapes.Circle,
    ) {
        Box(
            modifier = Modifier.size(RojanDimens.BackButtonSize),
            contentAlignment = Alignment.Center,
        ) {
            RojanIconContainer(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "بازگشت",
                tint = ManagerColors.TextPrimary,
                size = RojanIconSize.Medium,
            )
        }
    }
}
