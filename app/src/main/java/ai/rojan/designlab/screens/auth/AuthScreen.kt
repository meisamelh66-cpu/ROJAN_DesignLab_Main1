package ai.rojan.designlab.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import ai.rojan.designlab.R
import ai.rojan.designlab.domain.identity.SessionState
import ai.rojan.designlab.presentation.auth.AuthViewModel
import ai.rojan.designlab.presentation.auth.CustomerOtpStep
import ai.rojan.designlab.screens.customer.components.CustomerAccent
import ai.rojan.designlab.screens.customer.components.CustomerCardShape
import ai.rojan.designlab.screens.customer.components.CustomerHairline
import ai.rojan.designlab.screens.customer.components.CustomerScaffold
import ai.rojan.designlab.screens.customer.components.CustomerScreenMargin
import ai.rojan.designlab.screens.customer.components.CustomerSurfaceFill
import ai.rojan.designlab.screens.customer.components.CustomerTextField
import ai.rojan.designlab.screens.customer.components.RefPrimaryButton
import ai.rojan.designlab.screens.customer.hometheme.HomeColors
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * Customer Authentication — phone number → OTP → session
 * (`POST /api/v1/auth/otp/request` then `POST /api/v1/auth/otp/verify`),
 * a two-step flow (phone entry, then code entry) on one screen.
 *
 * Quiet Luxury pass (visual only): the `GlassBackButton` orb, the "سلام 🌸"
 * emoji headline, the 32dp `HomeGlassSurface` card, the violet `HomeTextField`
 * fields, and the gradient `PremiumButton` sitting inside a blurred violet
 * glow halo + drop shadow + white sheen are replaced with the [CustomerScaffold]
 * shell and flat foundation primitives: a plain headline, flat outlined
 * fields with a rose-gold cursor, an inline calm error line, and a solid
 * rose-gold [RefPrimaryButton].
 *
 * Brand consistency pass: a flat bordered header (same [CustomerCardShape] /
 * [CustomerSurfaceFill] / [CustomerHairline] treatment as the Home hero card)
 * carries the "ROJAN AI" wordmark, the approved slogan, and [AuthBrandLogoSlot].
 *
 * Login-screen brand mark fix: the portrait image ([AuthBrandLogoSlot], this
 * screen only) was replaced with the existing `R.drawable.rojan_ai_logo`
 * asset, shown via `ContentScale.Fit` (its own true aspect ratio, never
 * cropped/stretched) in the same 104x132dp slot the portrait occupied. Home's
 * hero card and Profile's cover fallback still use the original
 * [ai.rojan.designlab.screens.customer.HomeHeroPortraitSlot] portrait,
 * unchanged — this is a Login/entry-screen-only swap, not a shared-asset
 * change.
 *
 * NOTHING about the auth flow changed: every read of / call into
 * [AuthViewModel] — `sessionState`, `otpStep`, `errorMessage`, `isSubmitting`,
 * `editPhoneNumber`, `requestOtp`, `verifyOtp`, `resendOtp` — is byte-identical,
 * and the screen is still navigation-agnostic ([onExistingUserAuthenticated]
 * fires from a `LaunchedEffect` on `sessionState`). No ViewModel, OTP logic,
 * API call, navigation route, session restore, or repository is touched.
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

    val awaitingCode = otpStep is CustomerOtpStep.AwaitingCode

    CustomerScaffold(
        title = "ورود به روژان",
        onBackClick = {
            if (awaitingCode) {
                code = ""
                authViewModel.editPhoneNumber()
            } else {
                onBackClick()
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = CustomerScreenMargin, vertical = RojanDimens.SpaceLG),
        ) {
            AuthBrandHeader()

            Spacer(Modifier.height(RojanDimens.SpaceLG))

            Text(
                "ورود به روژان",
                style = RojanTypography.Display.copy(fontSize = 26.sp, lineHeight = 34.sp),
                color = HomeColors.TextPrimary,
            )
            Spacer(Modifier.height(RojanDimens.SpaceXS))
            Text(
                when (val step = otpStep) {
                    CustomerOtpStep.EnteringPhone -> "برای ادامه، شماره موبایل خود را وارد کنید"
                    is CustomerOtpStep.AwaitingCode -> "کد ارسال‌شده به ${step.phoneNumber} را وارد کنید"
                },
                style = RojanTypography.Body,
                color = HomeColors.TextSecondary,
            )

            Spacer(Modifier.height(RojanDimens.SpaceXL))

            when (otpStep) {
                CustomerOtpStep.EnteringPhone -> {
                    AuthField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = "شماره موبایل",
                        placeholder = "09123456789",
                        keyboardType = KeyboardType.Phone,
                        enabled = !isSubmitting,
                    )
                }

                is CustomerOtpStep.AwaitingCode -> {
                    AuthField(
                        value = code,
                        onValueChange = { code = it },
                        label = "کد تایید",
                        keyboardType = KeyboardType.NumberPassword,
                        enabled = !isSubmitting,
                    )
                    Spacer(Modifier.height(RojanDimens.SpaceMD))
                    AuthField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = "نام شما (اختیاری)",
                        keyboardType = KeyboardType.Text,
                        enabled = !isSubmitting,
                    )
                }
            }

            errorMessage?.let { message ->
                Spacer(Modifier.height(RojanDimens.SpaceMD))
                Text(
                    text = message,
                    style = RojanTypography.Caption,
                    color = RojanErrorText,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            Spacer(Modifier.height(RojanDimens.SpaceXL))

            RefPrimaryButton(
                label = when (otpStep) {
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
                // Root-cause fix: this button previously gave no feedback while
                // a real (bounded, but multi-second) OTP request/verify call was
                // in flight — just a slightly dimmed, inert button, indistinguishable
                // from a genuine hang. See RefPrimaryButton's own doc comment.
                loading = isSubmitting,
            )

            if (awaitingCode) {
                Spacer(Modifier.height(RojanDimens.SpaceSM))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = RojanDimens.MinTouchTarget)
                        .then(
                            if (!isSubmitting) {
                                Modifier.rojanPressable(
                                    onClick = { authViewModel.resendOtp() },
                                    role = Role.Button,
                                )
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "ارسال مجدد کد",
                        style = RojanTypography.Button,
                        color = CustomerAccent.copy(alpha = if (isSubmitting) 0.4f else 1f),
                    )
                }
            }
        }
    }
}

// --- Brand header (same treatment as the Home hero card) -----------------

@Composable
private fun AuthBrandHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CustomerCardShape)
            .background(CustomerSurfaceFill)
            .border(1.dp, CustomerHairline, CustomerCardShape)
            .padding(RojanDimens.SpaceMD),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "ROJAN AI",
                style = RojanTypography.SectionTitle.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = HomeColors.TextPrimary,
            )
            Spacer(Modifier.height(RojanDimens.SpaceXS))
            // Approved slogan, exact text — same string as the Home hero card.
            Text(
                "هوشمندتر مدیریت کن، زیباتر رشد کن",
                style = RojanTypography.Body,
                color = HomeColors.TextSecondary,
            )
        }
        Spacer(Modifier.width(RojanDimens.SpaceMD))
        AuthBrandLogoSlot()
    }
}

/**
 * Login/entry-screen-only brand mark: the existing `R.drawable.rojan_ai_logo`
 * asset, used exactly as-is (no crop/recolor/distortion — `ContentScale.Fit`
 * preserves its own true aspect ratio inside this slot rather than filling
 * it). Same footprint, shape, glow-tint background, and hairline border
 * [ai.rojan.designlab.screens.customer.HomeHeroPortraitSlot] used here
 * before, so the container/"premium sizing" of this header is unchanged —
 * only the image inside it changed. Deliberately local to this file, not a
 * change to the shared `HomeHeroPortraitSlot` (Home's hero card and
 * Profile's cover fallback still show the original portrait, untouched).
 */
@Composable
private fun AuthBrandLogoSlot() {
    val logoShape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .size(width = 104.dp, height = 132.dp)
            .clip(logoShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(HomeColors.Glow.copy(alpha = 0.35f), HomeColors.Magenta.copy(alpha = 0.12f)),
                ),
            )
            .border(1.dp, CustomerHairline, logoShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.rojan_ai_logo),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(RojanDimens.SpaceSM),
            contentScale = ContentScale.Fit,
        )
    }
}

// --- Flat outlined field -------------------------------------------------

// Phase 4 (P1): the flat labelled field is now `CustomerTextField` in the
// design system (extracted from this + the two SearchScreen copies). Same
// visual, RTL heuristic, focus/keyboard/enabled behaviour; the three call
// sites below are unchanged.
@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
) = CustomerTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    label = label,
    placeholder = placeholder,
    keyboardType = keyboardType,
    enabled = enabled,
)
