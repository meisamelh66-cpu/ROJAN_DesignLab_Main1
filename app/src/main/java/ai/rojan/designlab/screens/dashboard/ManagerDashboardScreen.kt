package ai.rojan.designlab.screens.dashboard

import ai.rojan.designlab.R
import ai.rojan.designlab.ui.theme.RojanTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview


/**
 * Landing destination for [ai.rojan.designlab.domain.model.Role.SALON_MANAGER],
 * reached via the Welcome screen's "مدیر سالن" role card.
 */
@Composable
fun ManagerDashboardScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
) {
    DashboardPlaceholder(
        title = stringResource(R.string.dashboard_manager_title),
        subtitle = stringResource(R.string.dashboard_manager_subtitle),
        modifier = modifier,
        onBackClick = onBackClick,
    )
}


@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun ManagerDashboardScreenPreview() {

    RojanTheme {

        ManagerDashboardScreen()

    }
}