package ai.rojan.designlab.screens.customer

import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.theme.RojanDimens
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Sprint 5A-3 — the Customer-app screen wrapper, counterpart to
 * `ManagerScaffold` / `ReceptionScaffold`: the dark [HomeBackgroundTheme]
 * canvas full-bleed behind the system bars, a `WindowInsets.safeDrawing`
 * content inset (status + nav + cutout + IME), and the shared
 * [GlassBackButton] pinned just below the status bar.
 *
 * Existing Customer screens still call [HomeBackgroundTheme] directly and
 * already get the identical `safeDrawing` inset via its
 * `applyContentInsets = true` default — this wrapper only spares *new*
 * screens from re-hand-rolling the `Box` / `Column` / back-button
 * structure. Migrating the existing direct callers onto it is deferred
 * design-system cleanup, not an edge-to-edge change.
 */
@Composable
fun CustomerScreenScaffold(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(RojanDimens.SpaceMD),
    content: @Composable () -> Unit,
) {
    HomeBackgroundTheme(applyContentInsets = false) {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(contentPadding),
            ) {
                content()
            }

            if (onBackClick != null) {
                GlassBackButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(RojanDimens.SpaceLG),
                )
            }
        }
    }
}
