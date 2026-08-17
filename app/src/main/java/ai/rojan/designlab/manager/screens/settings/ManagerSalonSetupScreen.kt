package ai.rojan.designlab.manager.screens.settings

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerIconContainer
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.presentation.settings.ManagerSalonSetupViewModel
import ai.rojan.designlab.manager.presentation.settings.SalonSetupFormState
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
 * Covers name/description/phone/email/address via the real `POST`/
 * `PUT /api/v1/salons` endpoints, plus latitude/longitude (Phase A
 * Correction) via the same `PUT` once a salon exists — real, live
 * backend fields (`Salon.updateProfile()`, verified directly against
 * `ROJAN_Backend` source), entered as plain coordinate text fields, not
 * a map or location picker. Latitude/longitude only appear in edit mode:
 * the backend's `CreateSalonRequest` has no such fields, so a brand-new
 * salon's coordinates can't be set until the owner edits it once it
 * exists - [SalonSetupForm] shows an explanatory caption in create mode
 * instead of a dead field that would silently drop what the owner typed.
 * `city` is still NOT sent to the backend - it has no backend field at
 * all. Logo/cover/gallery media (Central Salon Management — Salon Media
 * UI) now has a real navigation entry point,
 * [SalonMediaReferenceSection] below, reached the same way
 * [WorkingHoursEntryRow] already was — the backend upload infrastructure
 * it was previously waiting on has existed since Media Foundation Phase
 * 1; this only closes the Android-side gap.
 */
@Composable
fun ManagerSalonSetupScreen(
    viewModel: ManagerSalonSetupViewModel,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onSaved: () -> Unit = {},
    onWorkingHoursClick: (() -> Unit)? = null,
    onSalonMediaClick: (() -> Unit)? = null,
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
                    onLatitudeChange = viewModel::onLatitudeChange,
                    onLongitudeChange = viewModel::onLongitudeChange,
                    onSaveClick = { viewModel.save(onSaved = onSaved) },
                    // Owner Salon Profile Completion (Android-only) - only a
                    // real navigation entry point once a salon exists
                    // (edit mode); working hours are salon-scoped, so there's
                    // nothing to edit yet in create mode.
                    onWorkingHoursClick = if (state is UiState.Success) onWorkingHoursClick else null,
                    // Central Salon Management — Salon Media UI: same
                    // edit-mode-only gating as onWorkingHoursClick above -
                    // media is salon-scoped, so there's nothing to upload
                    // against until the salon exists.
                    onSalonMediaClick = if (state is UiState.Success) onSalonMediaClick else null,
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
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onWorkingHoursClick: (() -> Unit)? = null,
    onSalonMediaClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
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

        SalonCoordinatesSection(
            isCreateMode = isCreateMode,
            latitude = form.latitude,
            longitude = form.longitude,
            onLatitudeChange = onLatitudeChange,
            onLongitudeChange = onLongitudeChange,
            enabled = !isSubmitting,
        )

        if (onWorkingHoursClick != null) {
            WorkingHoursEntryRow(onClick = onWorkingHoursClick)
        }

        if (onSalonMediaClick != null) {
            SalonMediaEntryRow(onClick = onSalonMediaClick)
        }

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
 * Latitude/longitude editing (Phase A Correction) — real, live backend
 * fields (`Salon.updateProfile()`, verified directly against
 * `ROJAN_Backend` source), sent through
 * [ai.rojan.designlab.manager.domain.repository.ManagerSalonRepository.updateSalon]
 * exactly as typed. Plain coordinate text fields only — no map, no
 * location picker, per this correction's own scope. Only shown once a
 * salon exists ([isCreateMode] `false`): the backend's `CreateSalonRequest`
 * has no coordinate fields, so a brand-new salon's coordinates can't be
 * set until the owner edits it - a caption explains this in create mode
 * instead of silently dropping a value typed into a field that could
 * never actually be submitted yet.
 */
@Composable
private fun SalonCoordinatesSection(
    isCreateMode: Boolean,
    latitude: String,
    longitude: String,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    enabled: Boolean,
) {
    ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            Text(text = "موقعیت جغرافیایی", style = RojanTypography.CardTitle, color = ManagerColors.TextPrimary)
            if (isCreateMode) {
                Text(
                    text = "مختصات جغرافیایی پس از ثبت سالن قابل ویرایش است",
                    style = RojanTypography.Caption,
                    color = ManagerColors.TextSecondary,
                )
            } else {
                SalonTextField(label = "عرض جغرافیایی", value = latitude, onValueChange = onLatitudeChange, enabled = enabled)
                SalonTextField(label = "طول جغرافیایی", value = longitude, onValueChange = onLongitudeChange, enabled = enabled)
            }
        }
    }
}

/**
 * Owner Salon Profile Completion (Android-only) navigation entry point to
 * [ai.rojan.designlab.manager.screens.settings.ManagerWorkingHoursScreen] —
 * real backend contract already exists (`GET`/`PUT`/`DELETE
 * .../working-hours/{dayOfWeek}`).
 */
@Composable
private fun WorkingHoursEntryRow(onClick: () -> Unit) {
    ManagerGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .rojanPressable(onClick = onClick),
        shape = RojanShapes.GlassCard,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                ManagerIconContainer(imageVector = Icons.Filled.AccessTime, contentDescription = "ساعات کاری", containerSize = 32.dp)
                Text(text = "ساعات کاری", style = RojanTypography.CardTitle, color = ManagerColors.TextPrimary)
            }
            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = ManagerColors.TextSecondary)
        }
    }
}

/**
 * Central Salon Management — Salon Media UI navigation entry point, same
 * shape as [WorkingHoursEntryRow] just above. Routes to
 * [ai.rojan.designlab.manager.screens.settings.ManagerSalonMediaScreen].
 */
@Composable
private fun SalonMediaEntryRow(onClick: () -> Unit) {
    ManagerGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
                ManagerIconContainer(imageVector = Icons.Filled.Image, contentDescription = "لوگو و تصویر کاور", containerSize = 32.dp)
                Text(
                    text = "لوگو، کاور و گالری تصاویر",
                    style = RojanTypography.CardTitle,
                    color = ManagerColors.TextPrimary,
                )
            }
            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = ManagerColors.TextSecondary)
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
