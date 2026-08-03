package ai.rojan.designlab.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import ai.rojan.designlab.domain.identity.SessionState
import ai.rojan.designlab.presentation.auth.AuthViewModel
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
 * Android <-> Backend Full Integration milestone: real
 * `POST /api/v1/auth/login` / `/register` — the backend has no phone/OTP
 * concept, so the previous single-screen phone-number + SMS-OTP flow
 * (Booking Experience Refactor, Spec section 3) is replaced with
 * email/password(+full name for Register) on this same screen, same
 * visual system ([HomeGlassSurface]/[PremiumButton]/glow effects
 * untouched) — a mode toggle switches between Login and Register instead
 * of a navigation change, the same "stay on one screen" spirit the OTP
 * flow followed.
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
    val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()
    val isSubmitting by authViewModel.isSubmitting.collectAsStateWithLifecycle()

    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }

    LaunchedEffect(sessionState) {
        if (sessionState is SessionState.LoggedIn) onExistingUserAuthenticated()
    }

    HomeBackgroundTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceMD),
        ) {
            GlassBackButton(onClick = onBackClick)

            Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

            Text("سلام 🌸", style = RojanTypography.HeroTitle, color = HomeColors.TextPrimary)
            Spacer(modifier = Modifier.height(RojanDimens.SpaceXS))
            Text(
                if (isRegisterMode) "برای ساخت حساب کاربری، اطلاعات زیر را وارد کنید" else "برای ادامه، ایمیل و رمز عبور خود را وارد کنید",
                style = RojanTypography.Body,
                color = HomeColors.TextSecondary,
            )

            Spacer(modifier = Modifier.height(RojanDimens.SpaceLG))

            HomeGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(RojanDimens.SpaceMD),
                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                ) {
                    AnimatedVisibility(visible = isRegisterMode) {
                        HomeTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("نام و نام خانوادگی") },
                            enabled = !isSubmitting,
                            singleLine = true,
                            textStyle = LocalTextStyle.current.withDirectionFor(fullName),
                        )
                    }

                    HomeTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("ایمیل") },
                        placeholder = { Text("you@example.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        enabled = !isSubmitting,
                        singleLine = true,
                        textStyle = LocalTextStyle.current.withDirectionFor(email),
                    )

                    HomeTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("رمز عبور") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        enabled = !isSubmitting,
                        singleLine = true,
                    )

                    AnimatedVisibility(visible = !isRegisterMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = HomeColors.Glow,
                                    uncheckedColor = HomeColors.TextSecondary,
                                ),
                            )
                            Text("مرا به خاطر بسپار", style = RojanTypography.Body, color = HomeColors.TextSecondary)
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = RojanTypography.Caption,
                            color = RojanErrorText,
                        )
                    }

                    TextButton(onClick = {
                        isRegisterMode = !isRegisterMode
                        password = ""
                    }) {
                        Text(if (isRegisterMode) "قبلاً ثبت‌نام کرده‌اید؟ وارد شوید" else "حساب کاربری ندارید؟ ثبت‌نام کنید")
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
                    text = if (isRegisterMode) "ثبت‌نام" else "ورود",
                    onClick = {
                        if (isRegisterMode) {
                            authViewModel.register(email, password, fullName, rememberMe)
                        } else {
                            authViewModel.login(email, password, rememberMe)
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
