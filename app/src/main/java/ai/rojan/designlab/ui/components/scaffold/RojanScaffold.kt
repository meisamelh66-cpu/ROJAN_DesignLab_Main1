package ai.rojan.designlab.ui.components.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import ai.rojan.designlab.ui.background.PremiumBackground
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.theme.RojanDimens

/**
 * Post Build UX Flow & Design System Audit, Issue 2 — the shared
 * top-level screen wrapper the audit explicitly named (alongside
 * GlassSurface/GlassCard) as a required consolidation target.
 *
 * Every screen this session has hand-written the same structure
 * directly: `PremiumBackground { Column(padding = SpaceMD) {
 * GlassBackButton(...); ...content... } }`. That's fine as an
 * established pattern, but it's not a *shared component* — each screen
 * repeats the same padding value and back-button placement logic
 * itself rather than inheriting it from one place. This wraps exactly
 * that recurring structure once.
 *
 * Deliberately minimal: this does NOT replace [PremiumBackground] or
 * [ai.rojan.designlab.ui.components.glass.GlassSurface] (both stay
 * exactly as they are, used inside this scaffold's `content` like
 * always) - it only consolidates the *screen-level* wrapper around
 * them. [onBackClick] is optional (`null` default omits the back
 * button entirely) since not every screen has a previous level to
 * return to (Splash, Welcome, per the existing, deliberate exclusion
 * already established for [GlassBackButton]).
 */
@Composable
fun RojanScaffold(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(RojanDimens.SpaceMD),
    content: @Composable () -> Unit,
) {
    PremiumBackground {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                content()
            }

            if (onBackClick != null) {
                GlassBackButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(RojanDimens.SpaceLG),
                )
            }
        }
    }
}
