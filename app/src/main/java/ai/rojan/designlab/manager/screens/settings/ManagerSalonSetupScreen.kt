package ai.rojan.designlab.manager.screens.settings

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerIconContainer
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.presentation.settings.LocationCaptureMessage
import ai.rojan.designlab.manager.presentation.settings.ManagerSalonSetupViewModel
import ai.rojan.designlab.manager.presentation.settings.SalonSetupFormState
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.text.withDirectionFor
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanSuccessText
import ai.rojan.designlab.ui.theme.RojanTypography
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

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
 * Correction, later extended by the Manager Location Picker) via the
 * same `PUT` once a salon exists — real, live backend fields
 * (`Salon.updateProfile()`, verified directly against `ROJAN_Backend`
 * source). [SalonCoordinatesSection] captures them via the in-app map
 * picker ([ManagerLocationPickerMapDialog]) — the device's last-known
 * location only ever centers that map initially, the owner's confirmed
 * pin position is what's actually saved — with the underlying text
 * fields still editable for a manual correction. Latitude/longitude only
 * appear in edit mode:
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
    val loadState by viewModel.loadState.collectAsStateWithLifecycle()
    val form by viewModel.formState.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val submitError by viewModel.submitError.collectAsStateWithLifecycle()
    val isCapturingLocation by viewModel.isCapturingLocation.collectAsStateWithLifecycle()
    val locationCaptureMessage by viewModel.locationCaptureMessage.collectAsStateWithLifecycle()

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
                    isCapturingLocation = isCapturingLocation,
                    locationCaptureMessage = locationCaptureMessage,
                    onNameChange = viewModel::onNameChange,
                    onDescriptionChange = viewModel::onDescriptionChange,
                    onPhoneChange = viewModel::onPhoneChange,
                    onEmailChange = viewModel::onEmailChange,
                    onAddressChange = viewModel::onAddressChange,
                    onLatitudeChange = viewModel::onLatitudeChange,
                    onLongitudeChange = viewModel::onLongitudeChange,
                    onLocationCaptureStarted = viewModel::onLocationCaptureStarted,
                    onLocationCaptured = viewModel::onLocationCaptured,
                    onLocationCaptureFailed = viewModel::onLocationCaptureFailed,
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
    isCapturingLocation: Boolean,
    locationCaptureMessage: LocationCaptureMessage?,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onLocationCaptureStarted: () -> Unit,
    onLocationCaptured: (Double, Double) -> Unit,
    onLocationCaptureFailed: (String) -> Unit,
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
            modifier = Modifier.fillMaxWidth(),
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
            isCapturingLocation = isCapturingLocation,
            locationCaptureMessage = locationCaptureMessage,
            onLocationCaptureStarted = onLocationCaptureStarted,
            onLocationCaptured = onLocationCaptured,
            onLocationCaptureFailed = onLocationCaptureFailed,
        )

        if (onWorkingHoursClick != null) {
            WorkingHoursEntryRow(onClick = onWorkingHoursClick)
        }

        if (onSalonMediaClick != null) {
            SalonMediaEntryRow(onClick = onSalonMediaClick)
        }

        if (submitError != null) {
            Text(text = submitError, style = RojanTypography.Caption, color = RojanErrorText, modifier = Modifier.fillMaxWidth())
        }

        ManagerPrimaryButton(
            text = if (isCreateMode) "ثبت سالن" else "ذخیره تغییرات",
            enabled = !isSubmitting && form.name.isNotBlank() && form.phone.isNotBlank() && form.address.isNotBlank(),
            onClick = onSaveClick,
        )
    }
}

/**
 * Latitude/longitude (Phase A Correction) — real, live backend fields
 * (`Salon.updateProfile()`, verified directly against `ROJAN_Backend`
 * source), sent through
 * [ai.rojan.designlab.manager.domain.repository.ManagerSalonRepository.updateSalon]
 * exactly as captured. Only shown once a salon exists ([isCreateMode]
 * `false`): the backend's `CreateSalonRequest` has no coordinate fields,
 * so a brand-new salon's coordinates can't be set until the owner edits
 * it - a caption explains this in create mode instead of silently
 * dropping a value that could never actually be submitted yet.
 *
 * Manager Location Picker: the fields below stay plain, editable text
 * fields (a manual correction, or for an owner who prefers typing exact
 * survey coordinates, is still possible) - the "انتخاب موقعیت روی نقشه"
 * button opens [ManagerLocationPickerMapDialog], an in-app interactive
 * map. The device's last-known location (if available and permitted)
 * only ever centers that map when it opens - never the saved value by
 * itself. The owner pans the map until the fixed center pin sits on the
 * desired spot and taps confirm; THAT pin position is what reaches
 * [onLocationCaptured] and populates the fields below. There is no
 * external map app involved anywhere in this flow.
 */
@Composable
private fun SalonCoordinatesSection(
    isCreateMode: Boolean,
    latitude: String,
    longitude: String,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    enabled: Boolean,
    isCapturingLocation: Boolean,
    locationCaptureMessage: LocationCaptureMessage?,
    onLocationCaptureStarted: () -> Unit,
    onLocationCaptured: (Double, Double) -> Unit,
    onLocationCaptureFailed: (String) -> Unit,
) {
    val context = LocalContext.current
    var showMapPicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        // Opens the map regardless of the grant result - permission only ever
        // affects whether the map can center on the device's last-known
        // location; denial is handled gracefully, never as a hard failure,
        // per the Manager Location Picker's own requirement.
        showMapPicker = true
    }

    val openPicker: () -> Unit = openPicker@{
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
        if (!locationEnabled) {
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            onLocationCaptureFailed("لطفاً GPS دستگاه را فعال کرده و دوباره تلاش کنید، یا موقعیت را مستقیماً روی نقشه انتخاب کنید")
            return@openPicker
        }

        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFineLocation) {
            showMapPicker = true
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            Text(text = "موقعیت جغرافیایی", style = RojanTypography.CardTitle, color = ManagerColors.TextPrimary, modifier = Modifier.fillMaxWidth())
            if (isCreateMode) {
                Text(
                    text = "مختصات جغرافیایی پس از ثبت سالن قابل ویرایش است",
                    style = RojanTypography.Caption,
                    color = ManagerColors.TextSecondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = "موقعیت را روی نقشه مشخص کنید. مکان فعلی شما فقط برای مرکز اولیه نقشه استفاده می‌شود؛ مختصات نهایی همان پینی است که روی نقشه تأیید می‌کنید.",
                    style = RojanTypography.Caption,
                    color = ManagerColors.TextSecondary,
                    modifier = Modifier.fillMaxWidth(),
                )
                ManagerPrimaryButton(
                    text = "انتخاب موقعیت روی نقشه",
                    onClick = openPicker,
                    enabled = enabled && !isCapturingLocation,
                    loading = isCapturingLocation,
                )
                when (locationCaptureMessage) {
                    is LocationCaptureMessage.Success -> Text(
                        text = "موقعیت انتخابی شما ذخیره خواهد شد",
                        style = RojanTypography.Caption,
                        color = RojanSuccessText,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    is LocationCaptureMessage.Error -> Text(
                        text = locationCaptureMessage.text,
                        style = RojanTypography.Caption,
                        color = RojanErrorText,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    null -> Unit
                }
                SalonTextField(label = "عرض جغرافیایی", value = latitude, onValueChange = onLatitudeChange, enabled = enabled)
                SalonTextField(label = "طول جغرافیایی", value = longitude, onValueChange = onLongitudeChange, enabled = enabled)
            }
        }
    }

    if (showMapPicker) {
        ManagerLocationPickerMapDialog(
            initialLatitude = latitude.toDoubleOrNull(),
            initialLongitude = longitude.toDoubleOrNull(),
            onConfirm = { lat, lng ->
                showMapPicker = false
                onLocationCaptured(lat, lng)
            },
            onDismiss = { showMapPicker = false },
        )
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
            .heightIn(min = 64.dp)
            .rojanPressable(onClick = onClick),
        // RojanShapes.Small (16dp corners), matching
        // [ai.rojan.designlab.manager.components.QuickActionsSection]'s
        // already-working `QuickActionChip` for this same short-row shape.
        shape = RojanShapes.Small,
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
    // RTL fix: withDirectionFor below only governs the typed text's own
    // direction/alignment - it never reaches OutlinedTextField's internal
    // floating-label placement, which Material3 positions according to
    // ambient LayoutDirection (fixed LTR app-wide). Scoping Rtl to just this
    // one leaf field flips the label to the right without touching anything
    // else in this screen's layout.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            enabled = enabled,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = ManagerColors.TextPrimary).withDirectionFor(value),
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
}
