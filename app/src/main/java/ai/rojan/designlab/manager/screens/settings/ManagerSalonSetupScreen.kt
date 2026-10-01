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
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanSuccessText
import ai.rojan.designlab.ui.theme.RojanTypography
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

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
 * source). [SalonCoordinatesSection] captures them via the device's own
 * high-accuracy `LocationManager` fix (see [rememberLocationCapture]),
 * with the underlying text fields still editable for a manual
 * correction. Latitude/longitude only appear in edit mode:
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
    onLocationCaptured: (Double, Double, Float) -> Unit,
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
 * fields (a manual correction after a capture, or for an owner who
 * prefers typing exact survey coordinates, is still possible) - the
 * "دریافت موقعیت دقیق" button above them is the new acquisition path,
 * not a replacement for the fields themselves. See [rememberLocationCapture]
 * for the actual GPS flow and the authoritative-source architecture note
 * on why an external map app is never treated as returning a coordinate.
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
    onLocationCaptured: (Double, Double, Float) -> Unit,
    onLocationCaptureFailed: (String) -> Unit,
) {
    val captureLocation = rememberLocationCapture(
        onStarted = onLocationCaptureStarted,
        onCaptured = onLocationCaptured,
        onFailed = onLocationCaptureFailed,
    )

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
                Text(
                    text = "مختصات از GPS دستگاه شما دریافت و ذخیره می‌شود. نقشه فقط برای مشاهده و تأیید بصری موقعیت است.",
                    style = RojanTypography.Caption,
                    color = ManagerColors.TextSecondary,
                )
                ManagerPrimaryButton(
                    text = "دریافت موقعیت دقیق",
                    onClick = captureLocation,
                    enabled = enabled && !isCapturingLocation,
                    loading = isCapturingLocation,
                )
                when (locationCaptureMessage) {
                    is LocationCaptureMessage.Success -> Text(
                        text = "این موقعیت ذخیره خواهد شد (دقت: ${locationCaptureMessage.accuracyMeters.toInt()} متر)",
                        style = RojanTypography.Caption,
                        color = RojanSuccessText,
                    )
                    is LocationCaptureMessage.Error -> Text(
                        text = locationCaptureMessage.text,
                        style = RojanTypography.Caption,
                        color = RojanErrorText,
                    )
                    null -> Unit
                }
                SalonTextField(label = "عرض جغرافیایی", value = latitude, onValueChange = onLatitudeChange, enabled = enabled)
                SalonTextField(label = "طول جغرافیایی", value = longitude, onValueChange = onLongitudeChange, enabled = enabled)
            }
        }
    }
}

/**
 * Manager Location Picker: returns a trigger to run when "دریافت موقعیت
 * دقیق" is tapped.
 *
 * Architecture rule (explicit requirement, not a simplification): an
 * external map app is NEVER the source of the coordinate -
 * `ACTION_VIEW`/`geo:` intents have no result channel back to the caller
 * on Android, so there is no way for a map app to "return" a
 * user-selected point here. The device's own [LocationManager]
 * ([awaitAccurateLocation]) is the sole authoritative source, always.
 *
 * Flow order is deliberately GPS-first: [runLocationCaptureFlow] captures
 * the high-accuracy fix BEFORE calling [offerMapAppChooser], then opens
 * the map centered on that real captured coordinate (not a placeholder)
 * so the owner can visually confirm it on an actual map. That visual
 * confirmation is informational only - there is nothing for the owner to
 * "confirm back" into ROJAN; the coordinate that gets saved is the one
 * already captured, shown to the owner inline (see the accuracy caption
 * in [SalonCoordinatesSection]) before they ever leave the app. If the
 * pin looks wrong, the existing editable latitude/longitude fields are
 * the correction path - never a return value from the map app, which
 * does not exist.
 */
@Composable
private fun rememberLocationCapture(
    onStarted: () -> Unit,
    onCaptured: (Double, Double, Float) -> Unit,
    onFailed: (String) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            scope.launch { runLocationCaptureFlow(context, onStarted, onCaptured, onFailed) }
        } else {
            onFailed("برای دریافت موقعیت دقیق، دسترسی مکان لازم است")
        }
    }

    return remember(context) {
        {
            val hasFineLocation = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            if (hasFineLocation) {
                scope.launch { runLocationCaptureFlow(context, onStarted, onCaptured, onFailed) }
            } else {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            }
        }
    }
}

private suspend fun runLocationCaptureFlow(
    context: Context,
    onStarted: () -> Unit,
    onCaptured: (Double, Double, Float) -> Unit,
    onFailed: (String) -> Unit,
) {
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
        onFailed("لطفاً GPS دستگاه را فعال کرده و دوباره تلاش کنید")
        return
    }

    onStarted()
    val location = awaitAccurateLocation(locationManager)
    if (location != null) {
        offerMapAppChooser(context, location.latitude, location.longitude)
        onCaptured(location.latitude, location.longitude, location.accuracy)
    } else {
        onFailed("دریافت موقعیت دقیق ممکن نشد. دوباره تلاش کنید")
    }
}

/**
 * Optional visual aid only, shown AFTER the real GPS fix is already
 * captured so the map is centered on the owner's actual coordinate, not
 * a placeholder - lets the owner visually confirm their captured
 * location on a real map. Shows the Android app chooser when more than
 * one map app can handle a `geo:` intent, opens the single installed one
 * directly, or is a silent no-op with zero installed. Purely for the
 * owner's benefit: nothing the owner does in that external app changes
 * what gets saved - [latitude]/[longitude] (the already-captured GPS fix)
 * are what reach [onCaptured], unconditionally.
 */
private fun offerMapAppChooser(context: Context, latitude: Double, longitude: Double) {
    val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude"))
    val resolved = context.packageManager.queryIntentActivities(geoIntent, PackageManager.MATCH_DEFAULT_ONLY)
    try {
        when {
            resolved.isEmpty() -> Unit
            resolved.size == 1 -> context.startActivity(geoIntent)
            else -> context.startActivity(Intent.createChooser(geoIntent, "انتخاب برنامه نقشه"))
        }
    } catch (_: android.content.ActivityNotFoundException) {
        // No installed app can actually handle it despite resolving - skip silently,
        // this is a visual convenience only, never required for the capture above.
    }
}

/** Highest reported accuracy radius (meters) that ends the wait early instead of running the full timeout. */
private const val LOCATION_ACCURACY_THRESHOLD_METERS = 30f

/** Upper bound on how long the owner waits for a fix before getting the best reading obtained so far (or a failure, if none arrived at all). */
private const val LOCATION_TIMEOUT_MS = 20_000L

/**
 * Requests a single high-accuracy fix from [LocationManager] directly -
 * no Play Services dependency, since none exists in this project yet and
 * this flow doesn't need anything Play Services would add over a plain
 * GPS/network provider fix. Waits up to [LOCATION_TIMEOUT_MS] for a
 * reading at or below [LOCATION_ACCURACY_THRESHOLD_METERS] accuracy,
 * resolving early the moment one arrives; on timeout, returns the best
 * (lowest-accuracy-value) reading received during the wait rather than
 * discarding it, or `null` if the device never produced a single fix -
 * that `null` case is a genuine failure, surfaced as an error, never
 * silently treated as success with a placeholder coordinate.
 */
private suspend fun awaitAccurateLocation(locationManager: LocationManager): Location? {
    val criteria = Criteria().apply { accuracy = Criteria.ACCURACY_FINE }
    @Suppress("DEPRECATION")
    val provider = locationManager.getBestProvider(criteria, true) ?: return null

    return suspendCancellableCoroutine { continuation ->
        var bestLocation: Location? = null
        lateinit var listener: LocationListener
        lateinit var timeoutJob: Job

        listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val currentBest = bestLocation
                if (currentBest == null || location.accuracy < currentBest.accuracy) {
                    bestLocation = location
                }
                if (location.accuracy <= LOCATION_ACCURACY_THRESHOLD_METERS) {
                    timeoutJob.cancel()
                    locationManager.removeUpdates(this)
                    if (continuation.isActive) continuation.resume(location, onCancellation = null)
                }
            }

            @Deprecated("Deprecated in Java, still part of the LocationListener interface on minSdk 24")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }

        continuation.invokeOnCancellation {
            locationManager.removeUpdates(listener)
        }

        timeoutJob = CoroutineScope(continuation.context).launch {
            delay(LOCATION_TIMEOUT_MS)
            locationManager.removeUpdates(listener)
            if (continuation.isActive) continuation.resume(bestLocation, onCancellation = null)
        }

        try {
            locationManager.requestLocationUpdates(provider, 1000L, 0f, listener, Looper.getMainLooper())
        } catch (_: SecurityException) {
            timeoutJob.cancel()
            if (continuation.isActive) continuation.resume(null, onCancellation = null)
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
