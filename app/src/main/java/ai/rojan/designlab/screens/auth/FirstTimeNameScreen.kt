package ai.rojan.designlab.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.text.withDirectionFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import ai.rojan.designlab.domain.identity.SessionState
import ai.rojan.designlab.presentation.auth.AuthViewModel
import ai.rojan.designlab.ui.background.WarmBackground
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Booking Experience Refactor, Spec section 4 — First Time User.
 * "Ask ONLY ONE question... Nothing else. No surname. No email. No
 * birthday. No gender. No profile form." This screen has exactly one
 * input field and one button — nothing else is added, per that
 * constraint.
 *
 * Customer Journey Audit Phase A (P0-3) fix: [onBackClick] added. Without
 * it, a user who reached this screen and then used the system Back
 * gesture would land on [AuthScreen] while [ai.rojan.designlab.domain.identity.SessionState]
 * was still [SessionState.AwaitingFirstName] — a state that screen's
 * fields don't handle (phone field disabled, OTP field hidden), leaving
 * a dead, input-less screen. Calling [AuthViewModel.editPhoneNumber]
 * before popping back resets the session to [SessionState.LoggedOut]
 * first, so Back always lands on a live, usable phone-entry screen.
 */
@Composable
fun FirstTimeNameScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onNameSubmitted: () -> Unit,
) {
    val sessionState by authViewModel.sessionState.collectAsStateWithLifecycle()
    val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()
    var firstName by remember { mutableStateOf("") }

    LaunchedEffect(sessionState) {
        if (sessionState is SessionState.LoggedIn) onNameSubmitted()
    }

    WarmBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
        ) {
            GlassBackButton(onClick = {
                authViewModel.editPhoneNumber()
                onBackClick()
            })

            Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

            Text("سلام 🌸", style = RojanTypography.HeroTitle, color = RojanTextOnGlass)
            Spacer(modifier = Modifier.height(RojanDimens.SpaceXS))
            Text("من روژان هستم.", style = RojanTypography.Body, color = RojanTextOnGlass)
            Spacer(modifier = Modifier.height(RojanDimens.SpaceXS))
            Text("شما رو با چه اسمی صدا کنم؟", style = RojanTypography.Body, color = RojanTextOnGlass)

            Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("نام") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = LocalTextStyle.current.withDirectionFor(firstName),
                isError = errorMessage != null,
                supportingText = errorMessage?.let { { Text(it) } },
            )

            Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))

            PremiumButton(
                text = "ادامه",
                onClick = { authViewModel.submitFirstName(firstName) },
            )
        }
    }
}
