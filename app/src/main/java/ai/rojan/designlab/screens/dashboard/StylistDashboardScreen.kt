package ai.rojan.designlab.screens.dashboard

import ai.rojan.designlab.R
import ai.rojan.designlab.ui.theme.RojanTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview

/**
 * Landing destination for staff holding [ai.rojan.designlab.domain.identity.PersonRole.SPECIALIST] —
 * reached via the Welcome screen's "متخصص" card, which starts real
 * phone/OTP login; the actual role is resolved afterward from real
 * identity, not the card tap itself. See
 * [ai.rojan.designlab.navigation.RojanDestinations.STYLIST_ROLES].
 */
@Composable
fun StylistDashboardScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
) {
    DashboardPlaceholder(
        title = stringResource(R.string.dashboard_stylist_title),
        subtitle = stringResource(R.string.dashboard_stylist_subtitle),
        modifier = modifier,
        onBackClick = onBackClick,
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun StylistDashboardScreenPreview() {
    RojanTheme { StylistDashboardScreen() }
}
