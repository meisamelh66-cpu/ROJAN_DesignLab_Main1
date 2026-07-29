package ai.rojan.designlab.components.roles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.theme.RojanDarkSurfaceText
import ai.rojan.designlab.ui.theme.RojanMutedText


@Composable
fun RoleCard3D(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {

    GlassSurface(

        modifier = modifier
            .fillMaxHeight()
            .clickable {
                onClick()
            },

        shape = RoundedCornerShape(28.dp)

    ) {


        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),

            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.Center

        ) {


            Text(
                text = "✦",
                color = RojanDarkSurfaceText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )


            Spacer(
                modifier = Modifier.height(8.dp)
            )


            Text(
                text = title,
                color = RojanDarkSurfaceText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )


            Spacer(
                modifier = Modifier.height(6.dp)
            )


            Text(
                text = subtitle,
                color = RojanMutedText,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 3
            )

        }
    }
}