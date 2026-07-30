package ai.rojan.designlab.manager.screens.booking

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerIconContainer
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModel
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Manager Booking Journey Phase 2 — wizard entry screen. Resets
 * [ManagerBookingViewModel]'s state fresh every time a new booking
 * starts (in case a previous, abandoned session left stale selections),
 * then hands off to [ManagerBookingCustomerScreen] via [onStartClick].
 */
@Composable
fun ManagerBookingStartScreen(
    viewModel: ManagerBookingViewModel,
    onBackClick: (() -> Unit)? = null,
    onStartClick: () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        viewModel.reset()
    }

    ManagerScaffold(onBackClick = onBackClick) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ManagerIconContainer(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                containerSize = 72.dp,
                accentColor = ManagerColors.Turquoise,
            )

            Text(
                text = "نوبت جدید",
                style = RojanTypography.ScreenTitle,
                color = ManagerColors.TextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = RojanDimens.SpaceMD),
                textAlign = TextAlign.Center,
            )

            Text(
                text = "برای ثبت نوبت جدید، مشتری، خدمت، متخصص، تاریخ و ساعت را انتخاب کنید.",
                style = RojanTypography.Body,
                color = ManagerColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = RojanDimens.SpaceXS),
            )

            Spacer(modifier = Modifier.height(RojanDimens.SpaceXL))

            ManagerPrimaryButton(
                text = "شروع رزرو نوبت",
                onClick = onStartClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
