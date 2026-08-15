package ai.rojan.designlab.manager.screens.settings

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerIconContainer
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.presentation.settings.ManagerSalonSetupViewModel
import ai.rojan.designlab.manager.presentation.settings.SalonSetupFormState
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Manager App workspace — Owner Salon Identity setup/edit (First Salon
 * Pilot, Phase A). One screen for both create (owner has no salon yet,
 * [UiState.Empty]) and edit ([UiState.Success]) — same "one screen, mode
 * decided by loaded state" shape as
 * [ai.rojan.designlab.manager.screens.staff.ManagerStaffEditScreen], here
 * decided by whether `GET /salons/mine` returned a salon (via
 * [ManagerSalonSetupViewModel]) rather than a route-param sentinel, since
 * there is no list screen to navigate from with an id already known.
 *
 * Covers exactly the backend-ready fields today — name/description/phone/
 * email/address, via the real `POST`/`PUT /api/v1/salons` endpoints. `city`
 * and the logo/cover media references are NOT sent to the backend: per
 * `ROJAN_PhaseA_Salon_Identity_Readiness_Report_v1.md` §1/§7-8, none of the
 * three has a backend field to write to yet. [SalonMediaReferenceSection]
 * below is a reserved, honestly-labeled placeholder — Manager-themed
 * ([ManagerGlassSurface]/[ManagerColors]/[ManagerIconContainer]), not the
 * Customer-facing `RojanComingSoonState` (this module never reuses that
 * component — see [ai.rojan.designlab.manager.components.SalonIdentityCard]'s
 * own doc comment). No picker, no upload call, no local state persisted
 * for it — there is nothing real yet for it to do.
 */
@Composable
fun ManagerSalonSetupScreen(
    viewModel: ManagerSalonSetupViewModel,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onSaved: () -> Unit = {},
) {
    val loadState by viewModel.loadState.collectAsState()
    val form by viewModel.formState.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val submitError by viewModel.submitError.collectAsState()

    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        when (val state = loadState) {
            is UiState.Loading -> SalonSetupLoading()
            is UiState.Error -> SalonSetupLoadError(message = state.message, onRetry = viewModel::load)
            is UiState.Empty, is UiState.Success -> {
                SalonSetupForm(
                    isCreateMode = state is UiState.Empty,
                    form = form,
                    isSubmitting = isSubmitting,
                    submitError = submitError,
                    onNameChange = viewModel::onNameChange,
                    onDescriptionChange = viewModel::onDescriptionChange,
                    onPhoneChange = viewModel::onPhoneChange,
                    onEmailChange = viewModel::onEmailChange,
                    onAddressChange = viewModel::onAddressChange,
                    onSaveClick = { viewModel.save(onSaved = onSaved) },
                )
            }
        }
    }
}

@Composable
private fun SalonSetupLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(RojanDimens.SpaceXXL),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = ManagerColors.Turquoise)
    }
}

@Composable
private fun SalonSetupLoadError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(RojanDimens.SpaceMD),
        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, style = RojanTypography.Body, color = RojanErrorText)
        ManagerPrimaryButton(text = "تلاش مجدد", onClick = onRetry)
    }
}

@Composable
private fun SalonSetupForm(
    isCreateMode: Boolean,
    form: SalonSetupFormState,
    isSubmitting: Boolean,
    submitError: String?,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onSaveClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(RojanDimens.SpaceMD),
        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        Text(
            text = if (isCreateMode) "ثبت اطلاعات سالن" else "ویرایش اطلاعات سالن",
            style = RojanTypography.ScreenTitle,
            color = ManagerColors.TextPrimary,
        )

        ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RojanDimens.SpaceMD),
                verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
            ) {
                SalonTextField(label = "نام سالن", value = form.name, onValueChange = onNameChange, enabled = !isSubmitting)
                SalonTextField(label = "توضیحات", value = form.description, onValueChange = onDescriptionChange, enabled = !isSubmitting)
                SalonTextField(label = "شماره تماس", value = form.phone, onValueChange = onPhoneChange, enabled = !isSubmitting)
                SalonTextField(label = "ایمیل", value = form.email, onValueChange = onEmailChange, enabled = !isSubmitting)
                SalonTextField(label = "آدرس", value = form.address, onValueChange = onAddressChange, enabled = !isSubmitting)
            }
        }

        SalonMediaReferenceSection()

        if (submitError != null) {
            Text(text = submitError, style = RojanTypography.Caption, color = RojanErrorText)
        }

        ManagerPrimaryButton(
            text = if (isCreateMode) "ثبت سالن" else "ذخیره تغییرات",
            enabled = !isSubmitting && form.name.isNotBlank() && form.phone.isNotBlank() && form.address.isNotBlank(),
            onClick = onSaveClick,
        )
    }
}

/**
 * Media reference placeholder (logo/cover) — Phase A scope note: no
 * backend field, no upload endpoint, no storage decision made yet (see
 * `ROJAN_PhaseA_Salon_Identity_Readiness_Report_v1.md` §7-8). Static and
 * honest, not a fake picker — nothing here is sent to the backend or
 * persisted locally.
 */
@Composable
private fun SalonMediaReferenceSection() {
    ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
        ) {
            ManagerIconContainer(
                imageVector = Icons.Filled.Image,
                contentDescription = "لوگو و تصویر کاور",
                containerSize = 40.dp,
            )
            Text(text = "لوگو و تصویر کاور", style = RojanTypography.CardTitle, color = ManagerColors.TextPrimary)
            Text(
                text = "این بخش پس از آماده‌شدن زیرساخت آپلود رسانه در سرور فعال می‌شود",
                style = RojanTypography.Caption,
                color = ManagerColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun SalonTextField(label: String, value: String, onValueChange: (String) -> Unit, enabled: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(color = ManagerColors.TextPrimary),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = ManagerColors.TextPrimary,
            unfocusedTextColor = ManagerColors.TextPrimary,
            focusedBorderColor = ManagerColors.Turquoise,
            unfocusedBorderColor = ManagerColors.TextSecondary,
            focusedLabelColor = ManagerColors.Turquoise,
            unfocusedLabelColor = ManagerColors.TextSecondary,
            cursorColor = ManagerColors.Turquoise,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
