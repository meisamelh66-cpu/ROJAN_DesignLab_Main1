package ai.rojan.designlab.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
fun DashboardPlaceholder(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {

    Text(
        text = "$title\n$subtitle",
        modifier = modifier
    )

}