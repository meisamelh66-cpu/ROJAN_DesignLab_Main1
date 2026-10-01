package ai.rojan.designlab.manager.screens.staff

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.domain.media.ManagerMediaType
import ai.rojan.designlab.manager.domain.specialist.Specialist
import ai.rojan.designlab.manager.navigation.ManagerDestinations
import ai.rojan.designlab.presentation.common.userMessageFor
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.image.RojanRemoteImage
import ai.rojan.designlab.ui.media.CustomerImageBounds
import ai.rojan.designlab.ui.media.ImageOnlyPickerRequest
import ai.rojan.designlab.ui.media.decodeResizeAndCompress
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTheme
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.text.withDirectionFor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manager App workspace — one screen for both creating and editing a
 * [Specialist] (Manager Operational Foundation, Phase 6 Step 2;
 * Specialist Profile Expansion for the field set below).
 * [specialistId] == [ManagerDestinations.NEW_SPECIALIST_ID] means create;
 * any other value loads that specialist from
 * [ManagerRepositories.specialists] for editing — same "route param
 * doubles as a mode sentinel" shape [ManagerDestinations.STAFF_EDIT]
 * documents.
 *
 * Specialist Profile Expansion: [Specialist.name]/`bio`/`mobileNumber`/
 * `specialty` are all editable now, plus a real photo (uploaded through the
 * exact same pick → downscale → compress → multipart-upload pipeline
 * [ai.rojan.designlab.manager.screens.settings.ManagerSalonMediaScreen]
 * already established for salon logo/cover, reusing
 * [ai.rojan.designlab.manager.domain.media.ManagerMediaType.SPECIALIST_PHOTO] —
 * see that enum's own doc comment for why this needs no new backend
 * endpoint). A freshly-uploaded photo URL is held locally and only
 * actually attached to the specialist when the form's own Save button is
 * pressed, alongside every other field, matching this screen's existing
 * "one combined save" shape rather than Salon Media's per-slot auto-save
 * (which has no separate name/bio fields to coordinate with). `userId`
 * (when the backend already reports this specialist linked to a real user
 * account) is shown read-only - there is no "link specialist to user"
 * endpoint to drive from here, unlike Customer's own account-link flow.
 * [Specialist]'s former `skills`/`workingHours`/`commissionRate`
 * placeholders are gone; eligible-service ids (the real, already-defined
 * backend substitute for `skills`) are now fetched and shown read-only for
 * an existing specialist.
 *
 * [onSaved] fires after a successful create, update, or deactivate — in
 * every case the caller returns to the roster list.
 */
@Composable
fun ManagerStaffEditScreen(
    modifier: Modifier = Modifier,
    specialistId: String = ManagerDestinations.NEW_SPECIALIST_ID,
    onBackClick: (() -> Unit)? = null,
    onSaved: () -> Unit = {},
) {
    val context = LocalContext.current
    val isNew = specialistId == ManagerDestinations.NEW_SPECIALIST_ID
    val existing = remember(specialistId) {
        if (isNew) null else ManagerRepositories.specialists.getById(specialistId)
    }

    var name by remember(specialistId) { mutableStateOf(existing?.name.orEmpty()) }
    var bio by remember(specialistId) { mutableStateOf(existing?.bio.orEmpty()) }
    var mobileNumber by remember(specialistId) { mutableStateOf(existing?.mobileNumber.orEmpty()) }
    var specialty by remember(specialistId) { mutableStateOf(existing?.specialty.orEmpty()) }
    var photoUrl by remember(specialistId) { mutableStateOf(existing?.photoUrl) }
    var isUploadingPhoto by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // null = not yet loaded / not applicable (new specialist); empty list is
    // the backend's own "eligible for every service" meaning, not "none".
    var eligibleServiceCount by remember(specialistId) { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(specialistId) {
        if (!isNew) {
            ManagerRepositories.specialists.eligibleServiceIds(specialistId)
                .onSuccess { eligibleServiceCount = it.size }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let { picked ->
            scope.launch {
                isUploadingPhoto = true
                errorMessage = null
                val encoded = withContext(Dispatchers.Default) {
                    decodeResizeAndCompress(picked, context, maxDimension = CustomerImageBounds.AVATAR_MAX_DIMENSION)
                }
                if (encoded == null) {
                    isUploadingPhoto = false
                    errorMessage = "بارگذاری تصویر ناموفق بود"
                    return@launch
                }
                val (bytes, fileName, mimeType) = encoded
                val salonId = ManagerRepositories.salonId
                if (salonId == null) {
                    isUploadingPhoto = false
                    errorMessage = "سالن فعال یافت نشد"
                    return@launch
                }
                BackendApiContainerHolder.get(context).managerMediaRepository
                    .upload(salonId, ManagerMediaType.SPECIALIST_PHOTO, bytes, fileName, mimeType)
                    .onSuccess { asset -> photoUrl = asset.url }
                    .onFailure { errorMessage = userMessageFor(it) }
                isUploadingPhoto = false
            }
        }
    }

    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        Column(
            // Sprint 5A-3: scrollable so the form stays reachable when
            // ManagerScaffold's safeDrawing inset shrinks the area for the
            // keyboard.
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            Text(
                text = if (isNew) "متخصص جدید" else "ویرایش متخصص",
                style = RojanTypography.ScreenTitle,
                color = ManagerColors.TextPrimary,
                modifier = Modifier.fillMaxWidth(),
            )

            SpecialistPhotoPicker(
                photoUrl = photoUrl,
                isUploading = isUploadingPhoto,
                enabled = !isSubmitting,
                onPickClick = { photoPicker.launch(ImageOnlyPickerRequest) },
            )

            ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                ) {
                    StaffTextField(label = "نام متخصص", value = name, onValueChange = { name = it }, enabled = !isSubmitting)
                    StaffTextField(label = "تخصص", value = specialty, onValueChange = { specialty = it }, enabled = !isSubmitting)
                    StaffTextField(
                        label = "شماره موبایل",
                        value = mobileNumber,
                        onValueChange = { mobileNumber = it },
                        enabled = !isSubmitting,
                        keyboardType = KeyboardType.Phone,
                    )
                    StaffTextField(label = "بیوگرافی", value = bio, onValueChange = { bio = it }, enabled = !isSubmitting, singleLine = false)

                    if (!isNew && existing?.userId != null) {
                        Text(
                            text = "این متخصص به یک حساب کاربری متصل است.",
                            style = RojanTypography.Caption,
                            color = ManagerColors.TextSecondary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    eligibleServiceCount?.let { count ->
                        Text(
                            text = if (count == 0) "واجد شرایط برای تمام خدمات" else "واجد شرایط برای $count خدمت",
                            style = RojanTypography.Caption,
                            color = ManagerColors.TextSecondary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Text(text = errorMessage.orEmpty(), style = RojanTypography.Caption, color = RojanErrorText, modifier = Modifier.fillMaxWidth())
            }

            ManagerPrimaryButton(
                text = if (isNew) "ایجاد متخصص" else "ذخیره تغییرات",
                enabled = !isSubmitting && !isUploadingPhoto && name.isNotBlank(),
                onClick = {
                    errorMessage = null
                    isSubmitting = true
                    scope.launch {
                        val result = if (isNew) {
                            ManagerRepositories.specialists.create(
                                Specialist(
                                    id = "",
                                    name = name.trim(),
                                    bio = bio.trim().ifBlank { null },
                                    photoUrl = photoUrl,
                                    mobileNumber = mobileNumber.trim().ifBlank { null },
                                    specialty = specialty.trim().ifBlank { null },
                                    userId = null,
                                    active = true,
                                ),
                            )
                        } else {
                            ManagerRepositories.specialists.update(
                                (existing ?: return@launch).copy(
                                    name = name.trim(),
                                    bio = bio.trim().ifBlank { null },
                                    photoUrl = photoUrl,
                                    mobileNumber = mobileNumber.trim().ifBlank { null },
                                    specialty = specialty.trim().ifBlank { null },
                                ),
                            )
                        }
                        isSubmitting = false
                        result.onSuccess { onSaved() }
                            .onFailure { errorMessage = userMessageFor(it) }
                    }
                },
            )

            if (!isNew && existing?.active == true) {
                TextButton(
                    enabled = !isSubmitting,
                    onClick = {
                        errorMessage = null
                        isSubmitting = true
                        scope.launch {
                            ManagerRepositories.specialists.delete(specialistId)
                                .onSuccess { isSubmitting = false; onSaved() }
                                .onFailure { isSubmitting = false; errorMessage = userMessageFor(it) }
                        }
                    },
                ) {
                    Text("غیرفعال‌سازی این متخصص", color = RojanErrorText)
                }
            }
        }
    }
}

/**
 * Specialist photo - a circular preview (matching the ringed-avatar
 * treatment [ai.rojan.designlab.manager.components.SalonIdentityCard]
 * already established) when [photoUrl] is set, an honest icon placeholder
 * when it's `null` (never a fabricated image), and a spinner overlay while
 * [isUploading]. [onPickClick] launches the same Android Photo Picker every
 * other ROJAN image flow uses.
 */
@Composable
private fun SpecialistPhotoPicker(
    photoUrl: String?,
    isUploading: Boolean,
    enabled: Boolean,
    onPickClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .border(1.5.dp, ManagerColors.Turquoise, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (photoUrl != null) {
                RojanRemoteImage(
                    url = photoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(68.dp),
                    shape = CircleShape,
                    fallback = {
                        RojanIconContainer(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = ManagerColors.Turquoise,
                            size = RojanIconSize.Large,
                        )
                    },
                )
            } else {
                RojanIconContainer(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = ManagerColors.Turquoise,
                    size = RojanIconSize.Large,
                )
            }
            if (isUploading) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(ManagerColors.BaseDeep.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = ManagerColors.Turquoise, modifier = Modifier.size(28.dp))
                }
            }
        }

        TextButton(enabled = enabled && !isUploading, onClick = onPickClick) {
            Text(
                text = if (photoUrl == null) "افزودن عکس" else "تغییر عکس",
                color = ManagerColors.Turquoise,
            )
        }
    }
}

/** Minimal, self-contained Manager-themed text field — same styling as [ai.rojan.designlab.manager.screens.auth.ManagerOtpAuthScreen]'s own local `ManagerTextField` (no shared Manager text-field component exists yet to reuse). Generalized (Specialist Profile Expansion) from the original name-only field to take a [label]/[keyboardType]/[singleLine] so it covers name/specialty/mobile/bio alike. */
@Composable
private fun StaffTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
) {
    // RTL fix: scope Rtl layout direction to just this field so its
    // floating label right-anchors - withDirectionFor below only governs
    // the typed value's own direction/alignment, not the label's position.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            enabled = enabled,
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
)
@Composable
private fun ManagerStaffEditScreenPreview() {
    RojanTheme {
        ManagerStaffEditScreen()
    }
}
