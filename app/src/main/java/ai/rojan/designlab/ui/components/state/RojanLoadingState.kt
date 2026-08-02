package ai.rojan.designlab.ui.components.state

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * ROJAN AI shared loading-state primitive — Android <-> Backend Full
 * Integration milestone. Same card shape as [RojanEmptyState]/
 * [RojanErrorState] (sibling components, same [GlassSurface] wrapper) so a
 * screen's loading/empty/error states read as one consistent sequence
 * rather than three unrelated treatments. Fills the gap left by
 * `screens/customer/LoadingState.kt`, which was an empty, unused stub —
 * left in place rather than removed, since deleting it is outside this
 * milestone's data-layer-only scope.
 */
@Composable
fun RojanLoadingState(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RojanShapes.Small,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceXL),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = RojanTextOnGlass,
                strokeWidth = 2.5.dp,
            )

            if (message != null) {
                Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))

                Text(
                    text = message,
                    style = RojanTypography.Body,
                    color = RojanTextSecondary,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF18233A)
@Composable
private fun RojanLoadingStatePreview() {
    RojanLoadingState(message = "در حال بارگذاری...")
}
