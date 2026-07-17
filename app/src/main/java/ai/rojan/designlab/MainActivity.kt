package ai.rojan.designlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ai.rojan.designlab.navigation.RojanNavGraph
import ai.rojan.designlab.ui.theme.RojanTheme


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            RojanTheme {

                RojanNavGraph()

            }
        }
    }
}