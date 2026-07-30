package ai.rojan.designlab.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import ai.rojan.designlab.R
import ai.rojan.designlab.components.FrostedGlassOrb
import ai.rojan.designlab.components.PremiumLoadingBar

import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTheme

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope


@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit = {},
    minDisplayMillis: Long = 2600L,
) {

    var ready by remember {
        mutableStateOf(false)
    }

    val alpha = remember {
        Animatable(1f)
    }


    LaunchedEffect(Unit) {
        delay(minDisplayMillis)
        ready = true
    }


    LaunchedEffect(ready) {

        if (ready) {

            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 400,
                    easing = LinearEasing
                )
            )

            onSplashFinished()
        }
    }


    HomeBackgroundTheme {
        SplashScreenContent(
            modifier = Modifier.alpha(alpha.value)
        )
    }
}



@Composable
private fun SplashScreenContent(
    modifier: Modifier = Modifier
) {

    Box(

        modifier = modifier
            .fillMaxSize()

    ) {


        FrostedGlassOrb(

            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        start = 32.dp,
                        top = 96.dp
                    ),

            size = 64.dp,
            tint = HomeColors.Lavender,
            alpha = 0.4f
        )


        FrostedGlassOrb(

            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        end = 48.dp,
                        top = 160.dp
                    ),

            size = 36.dp,
            tint = Color.White,
            alpha = 0.55f
        )


        FrostedGlassOrb(

            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(
                        end = 24.dp
                    ),

            size = 80.dp,
            tint = HomeColors.Glow,
            alpha = 0.35f
        )


        FrostedGlassOrb(

            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = 40.dp,
                        bottom = 220.dp
                    ),

            size = 48.dp,
            tint = HomeColors.Rose,
            alpha = 0.4f
        )



        Column(

            modifier =
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(
                        horizontal = 32.dp
                    ),

            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center

        ) {


            SplashLogo(
                delayMillis = 0,
            )


            Spacer(
                modifier = Modifier.height(RojanDimens.SpaceMD)
            )


            SplashText(
                text = "ROJAN AI",
                delayMillis = 200,
                fontSize = 32.sp,
                weight = FontWeight.SemiBold,
                color = HomeColors.TextPrimary,
                spacing = 2.sp
            )


            Spacer(
                modifier = Modifier.height(RojanDimens.SpaceSM)
            )


            SplashText(
                text = "اکوسیستم هوشمند زیبایی",
                delayMillis = 450,
                fontSize = 14.sp,
                weight = FontWeight.Normal,
                color = HomeColors.TextSecondary,
                spacing = 0.5.sp
            )

        }



        Box(

            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        PaddingValues(
                            bottom = 64.dp
                        )
                    )
                    .width(120.dp)

        ){

            PremiumLoadingBar()

        }

    }
}



@Composable
private fun SplashLogo(
    delayMillis: Int,
) {

    val alpha =
        remember {
            Animatable(0f)
        }


    val y =
        remember {
            Animatable(10f)
        }


    LaunchedEffect(Unit) {

        delay(delayMillis.toLong())

        coroutineScope {

            launch {
                alpha.animateTo(
                    1f,
                    tween(
                        700,
                        easing = LinearEasing
                    )
                )
            }


            launch {

                y.animateTo(
                    0f,
                    tween(
                        700,
                        easing = LinearEasing
                    )
                )

            }

        }

    }


    Image(
        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
        contentDescription = "ROJAN AI",
        contentScale = ContentScale.Fit,

        modifier =
            Modifier
                .height(56.dp)
                .alpha(alpha.value)
                .offset(
                    y = y.value.dp
                )

    )

}



@Composable
private fun SplashText(
    text: String,
    delayMillis: Int,
    fontSize: TextUnit,
    weight: FontWeight,
    color: Color,
    spacing: TextUnit
){

    val alpha =
        remember {
            Animatable(0f)
        }


    val y =
        remember {
            Animatable(10f)
        }


    LaunchedEffect(Unit){

        delay(delayMillis.toLong())

        coroutineScope {

            launch {
                alpha.animateTo(
                    1f,
                    tween(
                        700,
                        easing = LinearEasing
                    )
                )
            }


            launch {

                y.animateTo(
                    0f,
                    tween(
                        700,
                        easing = LinearEasing
                    )
                )

            }

        }

    }


    Text(

        text = text,

        color = color,

        fontSize = fontSize,

        fontWeight = weight,

        letterSpacing = spacing,

        textAlign = TextAlign.Center,

        modifier =
            Modifier
                .alpha(alpha.value)
                .offset(
                    y = y.value.dp
                )

    )

}



@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun SplashPreview(){

    RojanTheme {

        SplashScreenContent()

    }

}