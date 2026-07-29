package ai.rojan.designlab.manager.components

import ai.rojan.designlab.R
import ai.rojan.designlab.components.brand.RojanLogo
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanGlassText
import ai.rojan.designlab.ui.theme.RojanTextOnDarkSurface
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Manager App workspace — dashboard header. Carries the official Manager
 * logo asset (`R.drawable.rojan_manager_logo` — pre-existing, not
 * recreated/redesigned here) beside the same [RojanLogo] wordmark
 * Customer App uses (kept, unmodified), with a Manager-only greeting/
 * notifications row built around it — no Customer screen, route, or
 * component is touched to build this.
 */
@Composable
fun ManagerHeader(
    modifier: Modifier = Modifier,
    managerName: String = "مدیر سالن",
    onNotificationsClick: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.rojan_manager_logo),
                contentDescription = "لوگوی مدیریت رویان",
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.width(RojanDimens.SpaceSM))
            RojanLogo()
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "خوش آمدید،",
                    style = RojanTypography.Caption,
                    color = RojanTextOnDarkSurface,
                )
                Text(
                    text = managerName,
                    style = RojanTypography.CardTitle,
                    color = RojanGlassText,
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(ManagerAccent.Teal.copy(alpha = 0.14f), CircleShape)
                    .rojanPressable(onClick = onNotificationsClick),
                contentAlignment = Alignment.Center,
            ) {
                RojanIconContainer(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "اعلان‌ها",
                    size = RojanIconSize.Medium,
                    tint = RojanGlassText,
                )
            }
        }
    }
}
