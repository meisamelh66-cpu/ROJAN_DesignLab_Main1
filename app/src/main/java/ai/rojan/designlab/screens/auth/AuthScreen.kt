package ai.rojan.designlab.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.text.withDirectionFor
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ai.rojan.designlab.ui.theme.RojanErrorText
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import ai.rojan.designlab.domain.identity.SessionState
import ai.rojan.designlab.presentation.auth.AuthViewModel
import ai.rojan.designlab.ui.background.WarmBackground
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanLuxurySecondaryBody
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextOnGlass
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Booking Experience Refactor, Spec section 3 — Authentication.
 * "Single screen: Phone Number / SMS Verification Code... Do NOT create
 * separate pages." Both fields genuinely live on this one screen: the
 * OTP field only appears (via [AnimatedVisibility], not a navigation
 * change) once [AuthViewModel.sessionState] becomes
 * [SessionState.AwaitingOtp].
 *
 * This screen is navigation-agnostic — it never calls a NavController
 * itself. [onExistingUserAuthenticated]/[onFirstTimeUser] are invoked
 * once, driven by a [LaunchedEffect] watching [AuthViewModel.sessionState],
 * exactly the same callback pattern every other screen in this codebase
 * uses.
 */
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onExistingUserAuthenticated: () -> Unit,
    onFirstTimeUser: () -> Unit,
) {
    val sessionState by authViewModel.sessionState.collectAsStateWithLifecycle()
    val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()

    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }

    LaunchedEffect(sessionState) {
        when (val state = sessionState) {
            is SessionState.LoggedIn -> onExistingUserAuthenticated()
            is SessionState.AwaitingFirstName -> onFirstTimeUser()
            else -> Unit
        }
    }

    WarmBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
        ) {
            GlassBackButton(onClick = onBackClick)

            Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

            Text("سلام 🌸", style = RojanTypography.HeroTitle, color = RojanTextOnGlass)
            Spacer(modifier = Modifier.height(RojanDimens.SpaceXS))
            Text(
                "برای ادامه، شماره موبایل خود را وارد کنید",
                style = RojanTypography.Body,
                color = RojanLuxurySecondaryBody,
            )

            Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

            GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(RojanDimens.SpaceMD),
                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                ) {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("شماره موبایل") },
                        placeholder = { Text("09xxxxxxxxx") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        enabled = sessionState is SessionState.LoggedOut,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.withDirectionFor(phoneNumber),
                    )

                    AnimatedVisibility(visible = sessionState is SessionState.AwaitingOtp) {
                        Column(verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                            OutlinedTextField(
                                value = otpCode,
                                onValueChange = { otpCode = it },
                                label = { Text("کد تایید") },
                                placeholder = { Text("۱۲۳۴") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.withDirectionFor(otpCode),
                            )

                            TextButton(onClick = { authViewModel.editPhoneNumber(); otpCode = "" }) {
                                Text("ویرایش شماره موبایل")
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = RojanTypography.Caption,
                            color = RojanErrorText,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))

            PremiumButton(
                text = if (sessionState is SessionState.AwaitingOtp) "تایید کد" else "ارسال کد",
                onClick = {
                    if (sessionState is SessionState.AwaitingOtp) {
                        authViewModel.submitOtp(otpCode)
                    } else {
                        authViewModel.submitPhoneNumber(phoneNumber)
                    }
                },
            )
        }
    }
}
