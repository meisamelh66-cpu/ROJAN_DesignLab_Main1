package ai.rojan.designlab.manager.components

import ai.rojan.designlab.R
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Manager App workspace — dashboard header. Carries the official
 * Manager logo asset (`R.drawable.rojan_manager_logo` — pre-existing,
 * not recreated/redesigned here).
 *
 * ROJAN AI Manager Visual Theme Implementation: the shared
 * [ai.rojan.designlab.components.brand.RojanLogo] wordmark hardcodes
 * dark-purple text (`RojanTextOnGlass`/`RojanTextSecondary`) meant for
 * Customer's light background — on the new dark luxury background that
 * text would be effectively invisible. Same "ROJAN AI" / Persian
 * subtitle content is kept, just rendered with [ManagerColors] instead
 * of calling that shared, non-parameterized component — `RojanLogo.kt`
 * itself and Customer's use of it are untouched.
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
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "ROJAN AI",
                    color = ManagerColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                Text(
                    text = "اکوسیستم هوشمند زیبایی",
                    color = ManagerColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }
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
                    color = ManagerColors.TextSecondary,
                )
                Text(
                    text = managerName,
                    style = RojanTypography.CardTitle,
                    color = ManagerColors.TextPrimary,
                )
            }

            ManagerIconContainer(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "اعلان‌ها",
                modifier = Modifier.rojanPressable(onClick = onNotificationsClick),
                containerSize = 44.dp,
            )
        }
    }
}
