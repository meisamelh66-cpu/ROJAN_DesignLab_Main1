package ai.rojan.designlab.manager.screens.profile

import ai.rojan.designlab.R
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanGlassText
import ai.rojan.designlab.ui.theme.RojanTextOnDarkSurface
import ai.rojan.designlab.ui.theme.RojanTheme
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Manager App workspace — profile placeholder. Carries the official
 * Manager logo asset (`R.drawable.rojan_manager_logo` — pre-existing,
 * not recreated/redesigned here) as the account avatar mark. Static
 * placeholder content, no backend/navigation wired yet — consistent
 * with the rest of the Manager module's foundation-first screens.
 */
@Composable
fun ManagerProfileScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    managerName: String = "مدیر سالن",
    salonName: String = "سالن رویان",
) {
    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = RojanDimens.SpaceXXL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Image(
                painter = painterResource(R.drawable.rojan_manager_logo),
                contentDescription = "لوگوی مدیریت رویان",
                modifier = Modifier.size(96.dp),
            )
            Text(
                text = managerName,
                style = RojanTypography.ScreenTitle,
                color = RojanGlassText,
                modifier = Modifier.padding(top = RojanDimens.SpaceMD),
            )
            Text(
                text = salonName,
                style = RojanTypography.Body,
                color = RojanTextOnDarkSurface,
                modifier = Modifier.padding(top = RojanDimens.SpaceXS),
            )
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
)
@Composable
private fun ManagerProfileScreenPreview() {
    RojanTheme {
        ManagerProfileScreen()
    }
}
