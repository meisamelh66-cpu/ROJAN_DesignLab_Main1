package ai.rojan.designlab.manager.screens.splash

import ai.rojan.designlab.R
import ai.rojan.designlab.ui.background.WarmBackground
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
 * Manager App workspace — splash placeholder. Carries the official
 * Manager logo asset (`R.drawable.rojan_manager_logo` — pre-existing,
 * not recreated/redesigned here), on the same warm-white canvas as the
 * frozen Manager Dashboard baseline. No timer/navigation wired yet — a
 * static placeholder, consistent with the rest of the Manager module's
 * foundation-first screens.
 */
@Composable
fun ManagerSplashScreen(modifier: Modifier = Modifier) {
    WarmBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceLG),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.rojan_manager_logo),
                contentDescription = "لوگوی مدیریت رویان",
                modifier = Modifier.size(120.dp),
            )
            Text(
                text = "پنل مدیریت رویان",
                style = RojanTypography.ScreenTitle,
                color = RojanGlassText,
                modifier = Modifier.padding(top = RojanDimens.SpaceLG),
            )
            Text(
                text = "اکوسیستم هوشمند زیبایی",
                style = RojanTypography.Caption,
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
private fun ManagerSplashScreenPreview() {
    RojanTheme {
        ManagerSplashScreen()
    }
}
