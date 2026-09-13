package ai.rojan.designlab.manager.components

import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

/**
 * TEAM2-002 (Manager Data Persistence). Dark-theme counterparts to
 * [ai.rojan.designlab.ui.components.state.RojanLoadingState]/
 * `RojanErrorState`/`RojanEmptyState` — those are built for light glass
 * cards on the Customer app's theme (their own doc comments: "dark text
 * ONLY on light cards") and would render illegibly on
 * [ManagerBackgroundTheme]'s dark luxury glass. Same card shape/spacing
 * language as every other Manager surface ([ManagerGlassSurface]), same
 * three-state shape the Customer-side components already established —
 * introduced because this task is the first place the Manager app needs
 * a real Loading/Error/Empty state at all.
 */
@Composable
fun ManagerLoadingState(modifier: Modifier = Modifier, message: String? = null) {
    ManagerGlassSurface(modifier = modifier.fillMaxWidth(), shape = RojanShapes.Small) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceXL),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = ManagerColors.Turquoise)
            if (message != null) {
                Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))
                Text(message, style = RojanTypography.Body, color = ManagerColors.TextSecondary, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun ManagerErrorState(
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    ManagerGlassSurface(modifier = modifier.fillMaxWidth(), shape = RojanShapes.Small) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceXL),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("مشکلی پیش آمد", style = RojanTypography.CardTitle, color = ManagerColors.TextPrimary, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(RojanDimens.SpaceXS))
            Text(description, style = RojanTypography.Body, color = ManagerColors.TextSecondary, textAlign = TextAlign.Center)
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))
                ManagerPrimaryButton(text = actionLabel, onClick = onAction)
            }
        }
    }
}

@Composable
fun ManagerEmptyState(title: String, modifier: Modifier = Modifier, description: String? = null) {
    ManagerGlassSurface(modifier = modifier.fillMaxWidth(), shape = RojanShapes.Small) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceXL),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = RojanTypography.CardTitle, color = ManagerColors.TextPrimary, textAlign = TextAlign.Center)
            if (description != null) {
                Spacer(modifier = Modifier.height(RojanDimens.SpaceXS))
                Text(description, style = RojanTypography.Body, color = ManagerColors.TextSecondary, textAlign = TextAlign.Center)
            }
        }
    }
}
