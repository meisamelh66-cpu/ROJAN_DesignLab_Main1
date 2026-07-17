package ai.rojan.app.ui.components.effects

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp


@Composable
fun GlowBox(
    modifier: Modifier = Modifier,
    radius: Dp,
    color: Color,
    content: @Composable () -> Unit
) {


    Box(

        modifier = modifier.shadow(
            elevation = radius,
            ambientColor = color,
            spotColor = color
        )

    ){

        content()

    }

}