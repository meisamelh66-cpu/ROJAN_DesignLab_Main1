package ai.rojan.designlab.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import ai.rojan.designlab.ui.background.PremiumBackground
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Booking Experience Refactor, Spec section 4 — First Time User.
 * "Ask ONLY ONE question... Nothing else. No surname. No email. No
 * birthday. No gender. No profile form." This screen has exactly one
 * input field and one button — nothing else is added, per that
 * constraint.
 */
@Composable
fun FirstTimeNameScreen(
    authViewModel: AuthViewModel,
    onNameSubmitted: () -> Unit,
) {
    val sessionState by authViewModel.sessionState.collectAsStateWithLifecycle()
    val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()
    var firstName by remember { mutableStateOf("") }

    LaunchedEffect(sessionState) {
        if (sessionState is SessionState.LoggedIn) onNameSubmitted()
    }

    PremiumBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
        ) {
            Spacer(modifier = Modifier.height(RojanDimens.SpaceXL))

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
