package ai.rojan.designlab.manager.screens.splash

import ai.rojan.designlab.R
import ai.rojan.designlab.manager.components.ManagerBackgroundTheme
import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTheme
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Manager App workspace — splash entry point. Carries the official
 * Manager logo asset (`R.drawable.rojan_manager_logo` — pre-existing,
 * not recreated/redesigned here).
 *
 * ROJAN AI Manager Visual Theme Implementation: re-themed onto
 * [ManagerBackgroundTheme] (dark luxury glass) instead of the shared
 * `WarmBackground` — content/timer behavior unchanged.
 */
@Composable
fun ManagerSplashScreen(
    modifier: Modifier = Modifier,
    onSplashFinished: () -> Unit = {},
    minDisplayMillis: Long = 1600L,
) {
    LaunchedEffect(Unit) {
        delay(minDisplayMillis)
        onSplashFinished()
    }

    ManagerBackgroundTheme(modifier = modifier.fillMaxSize()) {
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
                color = ManagerColors.TextPrimary,
                modifier = Modifier.padding(top = RojanDimens.SpaceLG),
            )
            Text(
                text = "اکوسیستم هوشمند زیبایی",
                style = RojanTypography.Caption,
                color = ManagerColors.TextSecondary,
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
