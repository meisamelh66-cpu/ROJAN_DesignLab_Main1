package ai.rojan.designlab.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.TextButton
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.text.withDirectionFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import ai.rojan.designlab.ui.theme.RojanErrorText
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import ai.rojan.designlab.domain.identity.SessionState
import ai.rojan.designlab.presentation.auth.AuthViewModel
import ai.rojan.designlab.presentation.auth.CustomerOtpStep
import ai.rojan.designlab.screens.customer.hometheme.HomeBackgroundTheme
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface
import ai.rojan.designlab.screens.customer.hometheme.HomeTextField
import ai.rojan.designlab.ui.components.buttons.PremiumButton
import ai.rojan.designlab.ui.components.navigation.GlassBackButton
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Customer Authentication Migration: the only Customer authentication
 * method is phone number -> OTP -> session, same real backend endpoints
 * ([ai.rojan.designlab.manager.screens.auth.ManagerOtpAuthScreen] already
 * uses for Manager) — `POST /api/v1/auth/otp/request` then
 * `POST /api/v1/auth/otp/verify`. Replaces the previous email/password (+
 * Register toggle + Remember Me checkbox) screen entirely, same visual
 * system ([HomeGlassSurface]/[PremiumButton]/glow effects untouched) — a
 * two-step flow (phone entry, then code entry) instead of a mode toggle,
 * matching Manager's own step shape ([CustomerOtpStep]) while staying on
 * this one screen/composable, same "stay on one screen" spirit the
 * previous mode-toggle followed.
 *
 * The optional name field on the code-entry step is the real,
 * backend-compatible replacement for the old (demo-only, unreachable)
 * `FirstTimeNameScreen` — see [AuthViewModel.verifyOtp]'s own doc comment
 * for why a separate post-verify screen isn't possible here.
 *
 * This screen is navigation-agnostic — it never calls a NavController
 * itself. [onExistingUserAuthenticated] is invoked once, driven by a
 * [LaunchedEffect] watching [AuthViewModel.sessionState], the same
 * callback pattern every other screen in this codebase uses.
 */
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onExistingUserAuthenticated: () -> Unit,
) {
    val sessionState by authViewModel.sessionState.collectAsStateWithLifecycle()
    val otpStep by authViewModel.otpStep.collectAsStateWithLifecycle()
    val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()
    val isSubmitting by authViewModel.isSubmitting.collectAsStateWithLifecycle()

    var phoneNumber by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }

    LaunchedEffect(sessionState) {
        if (sessionState is SessionState.LoggedIn) onExistingUserAuthenticated()
    }

    HomeBackgroundTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
        ) {
            GlassBackButton(onClick = {
                if (otpStep is CustomerOtpStep.AwaitingCode) {
                    code = ""
                    authViewModel.editPhoneNumber()
                } else {
                    onBackClick()
                }
            })

            Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

            Text("سلام 🌸", style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary)
            Spacer(modifier = Modifier.height(RojanDimens.SpaceXS))
            Text(
                when (val step = otpStep) {
                    CustomerOtpStep.EnteringPhone -> "برای ادامه، شماره موبایل خود را وارد کنید"
                    is CustomerOtpStep.AwaitingCode -> "کد ارسال‌شده به ${step.phoneNumber} را وارد کنید"
                },
                style = RojanTypography.Body,
                color = HomeColors.TextSecondary,
            )

            Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

            HomeGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                AnimatedContent(targetState = otpStep, label = "auth_otp_step") { step ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RojanDimens.SpaceMD),
                        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                    ) {
                        when (step) {
                            CustomerOtpStep.EnteringPhone -> {
                                HomeTextField(
                                    value = phoneNumber,
                                    onValueChange = { phoneNumber = it },
                                    label = { Text("شماره موبایل") },
                                    placeholder = { Text("09123456789") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    enabled = !isSubmitting,
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.withDirectionFor(phoneNumber),
                                )
                            }

                            is CustomerOtpStep.AwaitingCode -> {
                                HomeTextField(
                                    value = code,
                                    onValueChange = { code = it },
                                    label = { Text("کد تایید") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    enabled = !isSubmitting,
                                    singleLine = true,
                                )
                                HomeTextField(
                                    value = fullName,
                                    onValueChange = { fullName = it },
                                    label = { Text("نام شما (اختیاری)") },
                                    enabled = !isSubmitting,
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.withDirectionFor(fullName),
                                )
                                TextButton(onClick = { authViewModel.resendOtp() }, enabled = !isSubmitting) {
                                    Text("ارسال مجدد کد", color = HomeColors.Glow)
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
            }

            Spacer(modifier = Modifier.height(RojanDimens.SpaceMD))

            // Rose Gold ambient glow behind the CTA — same light direction/
            // color language as the panel above, so the button reads as
            // "floating" too rather than sitting flat under the card.
            // `PremiumButton` itself (gradient fill, press state) is
            // untouched; this only adds elevation around it. Visual
            // Intensity Correction v2: the first pass's halo (0.38f
            // alpha, clipped to bounds) barely registered — enlarged,
            // brightened, and switched to `BlurredEdgeTreatment.Unbounded`
            // so it visibly bleeds past the button's own silhouette
            // instead of stopping exactly at its edge. Added a genuine
            // dark contact shadow beneath (same technique as
            // [CustomerGlassSurface]) and a soft top-highlight sheen on
            // top, for "premium shadow and light interaction."
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(RojanDimens.ButtonHeight + 24.dp)
                        .blur(36.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    HomeColors.Glow.copy(alpha = 0.65f),
                                    HomeColors.Glow.copy(alpha = 0f),
                                ),
                            ),
                            shape = RojanShapes.PremiumButton,
                        ),
                )

                Box(
                    modifier = Modifier
                        .size(width = RojanDimens.ButtonWidth, height = RojanDimens.ButtonHeight)
                        .shadow(
                            elevation = 14.dp,
                            shape = RojanShapes.PremiumButton,
                            ambientColor = Color.Black.copy(alpha = 0.30f),
                            spotColor = Color.Black.copy(alpha = 0.26f),
                        ),
                )

                PremiumButton(
                    text = when (otpStep) {
                        CustomerOtpStep.EnteringPhone -> "ارسال کد تایید"
                        is CustomerOtpStep.AwaitingCode -> "تایید و ورود"
                    },
                    onClick = {
                        when (otpStep) {
                            CustomerOtpStep.EnteringPhone -> authViewModel.requestOtp(phoneNumber)
                            is CustomerOtpStep.AwaitingCode -> authViewModel.verifyOtp(code, fullName)
                        }
                    },
                    enabled = !isSubmitting,
                )

                // Soft top-highlight sheen — a light-catching cue on top
                // of the button surface, non-interactive (no click
                // handling), so `PremiumButton`'s own press behavior
                // underneath is unaffected.
                Box(
                    modifier = Modifier
                        .size(width = RojanDimens.ButtonWidth, height = RojanDimens.ButtonHeight)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.30f),
                                    Color.White.copy(alpha = 0f),
                                ),
                            ),
                            shape = RojanShapes.PremiumButton,
                        ),
                )
            }
        }
    }
}
