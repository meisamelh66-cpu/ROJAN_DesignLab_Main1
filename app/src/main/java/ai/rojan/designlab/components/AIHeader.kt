package ai.rojan.designlab.components


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight


import ai.rojan.designlab.ui.theme.RojanDarkSurfaceText
import ai.rojan.designlab.ui.theme.RojanHeroGradient
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary


@Composable
fun AIHeader(

    modifier: Modifier = Modifier

) {


    Column(

        modifier = modifier,

        horizontalAlignment = Alignment.CenterHorizontally

    ) {


        Box(

            modifier = Modifier
                .clip(
                    RoundedCornerShape(16.dp)
                )
                .background(
                    RojanHeroGradient
                )
                .padding(
                    horizontal = 24.dp,
                    vertical = 16.dp
                ),

            contentAlignment = Alignment.Center

        ) {


            Text(

                text = "ROJAN",

                color = RojanDarkSurfaceText,

                fontSize = 20.sp,

                fontWeight = FontWeight.Bold

            )

        }



        Text(

            text = "هوش مصنوعی زیبایی",

            modifier = Modifier.padding(
                top = 12.dp
            ),

            color = RojanTextPrimary,

            fontSize = 20.sp,

            fontWeight = FontWeight.Bold

        )



        Text(

            text = "مدیریت هوشمند سالن زیبایی",

            modifier = Modifier.padding(
                top = 6.dp
            ),

            color = RojanTextSecondary,

            fontSize = 14.sp

        )


    }

}