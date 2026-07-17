package ai.rojan.designlab.ui.animation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable


object RojanAnimations {


    val Enter =
        tween<Float>(
            durationMillis = 700,
            easing = FastOutSlowInEasing
        )


    val Glow =
        tween<Float>(
            durationMillis = 1500,
            easing = FastOutSlowInEasing
        )


}


@Composable
fun rememberScaleAnimation(
    visible:Boolean
):Float{


    return animateFloatAsState(

        targetValue =
            if(visible)
                1f
            else
                0.85f,

        animationSpec =
            RojanAnimations.Enter

    ).value

}