package ai.rojan.designlab.manager.screens.booking

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerIconContainer
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Manager Booking Journey Phase 2 — final step. Purely a confirmation
 * message; [onDoneClick] returns to Dashboard (wired in
 * [ai.rojan.designlab.manager.navigation.ManagerNavGraph] to pop the
 * whole wizard off the back stack).
 */
@Composable
fun ManagerBookingSuccessScreen(
    onDoneClick: () -> Unit = {},
) {
    ManagerScaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ManagerIconContainer(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                containerSize = 72.dp,
                accentColor = ManagerColors.Turquoise,
            )

            Text(
                text = "نوبت با موفقیت ثبت شد",
                style = RojanTypography.ScreenTitle,
                color = ManagerColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = RojanDimens.SpaceMD),
            )

            Text(
                text = "نوبت جدید در تقویم سالن ثبت و قابل مشاهده است.",
                style = RojanTypography.Body,
                color = ManagerColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = RojanDimens.SpaceXS),
            )

            Spacer(modifier = Modifier.height(RojanDimens.SpaceXL))

            ManagerPrimaryButton(
                text = "بازگشت به داشبورد",
                onClick = onDoneClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
