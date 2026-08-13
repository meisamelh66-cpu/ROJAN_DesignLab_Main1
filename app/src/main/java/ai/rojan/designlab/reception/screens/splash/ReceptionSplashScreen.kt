package ai.rojan.designlab.reception.screens.splash

import ai.rojan.designlab.R
import ai.rojan.designlab.ui.background.WarmBackground
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.ReceptionPalette
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
 * Reception App workspace — splash entry point. Uses the generic
 * `R.mipmap.ic_launcher_foreground` mark (the same placeholder Customer's
 * own [ai.rojan.designlab.screens.splash.SplashScreen] falls back to) —
 * no bespoke Reception logo exists yet (see `app/build.gradle.kts`'s
 * flavor comment), so this deliberately does not fabricate one.
 *
 * See ROJAN_Reception_Implementation_Plan_v1.md, Phase 0.
 */
@Composable
fun ReceptionSplashScreen(
    modifier: Modifier = Modifier,
    onSplashFinished: () -> Unit = {},
    minDisplayMillis: Long = 1600L,
) {
    LaunchedEffect(Unit) {
        delay(minDisplayMillis)
        onSplashFinished()
    }

    WarmBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceLG),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = "لوگوی پذیرش رویان",
                modifier = Modifier.size(120.dp),
            )
            Text(
                text = "پذیرش رویان",
                style = RojanTypography.ScreenTitle,
                color = ReceptionPalette.textPrimary,
                modifier = Modifier.padding(top = RojanDimens.SpaceLG),
            )
            Text(
                text = "اکوسیستم هوشمند زیبایی",
                style = RojanTypography.Caption,
                color = ReceptionPalette.textSecondary,
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
private fun ReceptionSplashScreenPreview() {
    RojanTheme {
        ReceptionSplashScreen()
    }
}
