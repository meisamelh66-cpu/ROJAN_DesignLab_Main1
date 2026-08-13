package ai.rojan.designlab.reception.components

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
 * Reception App workspace screen wrapper — same structure as
 * [ai.rojan.designlab.ui.components.scaffold.RojanScaffold] (left
 * untouched), but built on the shared [WarmBackground] rather than
 * [ai.rojan.designlab.ui.background.PremiumBackground]'s decorative salon
 * photo (not appropriate behind operational staff screens) or a bespoke
 * dark theme like Manager's [ai.rojan.designlab.manager.components.ManagerScaffold]
 * (Manager's dark background came from an approved design reference —
 * `design/reference/ROJAN_Manager_Reference.png` — no equivalent exists
 * for Reception yet, so this deliberately reuses the shared, already-
 * approved light background instead of inventing a new visual system).
 * The shared [GlassBackButton] (white glass + dark-purple icon) is correct
 * as-is on this light background, unlike Manager's case.
 *
 * See ROJAN_Reception_Implementation_Plan_v1.md, Phase 0.
 */
@Composable
fun ReceptionScaffold(
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
