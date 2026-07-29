package ai.rojan.designlab.manager.components

import ai.rojan.designlab.ui.background.WarmBackground
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.theme.RojanDimens
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Manager App workspace screen wrapper — same structure as
 * [ai.rojan.designlab.ui.components.scaffold.RojanScaffold] (left
 * untouched; still used by Customer/legacy screens), but built on
 * [WarmBackground] instead of [ai.rojan.designlab.ui.background.PremiumBackground].
 *
 * Readability theme update: the Manager Dashboard's visual direction
 * calls for a warm white background with maximum text contrast —
 * [WarmBackground] is the exact existing, shared component built for
 * that (solid `RojanWarmWhite` base, a barely-there 0.14f pastel wash,
 * one quiet 0.05f glow zone), already used by Customer screens from
 * Member Salons List onward. `RojanScaffold` has no parameter to swap
 * its hardcoded `PremiumBackground`, so this is a small, isolated
 * Manager-only duplicate of its layout — not a fork or edit of the
 * shared scaffold itself.
 */
@Composable
fun ManagerScaffold(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(RojanDimens.SpaceMD),
    content: @Composable () -> Unit,
) {
    WarmBackground {
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
