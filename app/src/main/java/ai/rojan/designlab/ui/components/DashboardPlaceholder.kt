package ai.rojan.designlab.ui.components

import ai.rojan.designlab.ui.text.Text
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