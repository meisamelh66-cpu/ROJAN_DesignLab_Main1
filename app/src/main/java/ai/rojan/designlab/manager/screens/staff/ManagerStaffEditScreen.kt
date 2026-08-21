package ai.rojan.designlab.manager.screens.staff

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.components.ManagerTargetedMediaGallery
import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.domain.media.ManagerMediaType
import ai.rojan.designlab.manager.domain.specialist.Specialist
import ai.rojan.designlab.manager.navigation.ManagerDestinations
import ai.rojan.designlab.presentation.common.userMessageFor
import ai.rojan.designlab.ui.components.image.RojanRemoteImage
import ai.rojan.designlab.ui.components.image.decodeResizeAndCompress
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTheme
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.text.Text
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manager App workspace — one screen for both creating and editing a
 * [Specialist] (Manager Operational Foundation, Phase 6 Step 2).
 * [specialistId] == [ManagerDestinations.NEW_SPECIALIST_ID] means create;
 * any other value loads that specialist from
 * [ManagerRepositories.specialists] for editing — same "route param
 * doubles as a mode sentinel" shape [ManagerDestinations.STAFF_EDIT]
 * documents.
 *
 * [Specialist.name] and, as of Media Sprint P0, [Specialist.photoUrl] are
 * editable — `bio` still isn't (see
 * [ai.rojan.designlab.manager.data.BackendSpecialistRepository]'s own doc
 * comment on why `skills`/`workingHours`/`commissionRate` stay disclosed
 * placeholders); not adding fields here that wouldn't actually persist
 * anything real.
 *
 * Avatar upload reuses the exact same Media Foundation pipeline the Salon
 * Media screen already uses ([ai.rojan.designlab.manager.domain.media.ManagerMediaType.SPECIALIST_PHOTO]
 * via [ai.rojan.designlab.di.BackendApiContainer.managerMediaRepository])
 * — no new upload mechanism, no schema change. Unlike logo/cover, there is
 * no separate "assign identity slot" step: the returned media URL is held
 * in local state and only actually persisted onto the specialist when the
 * caller taps Save (create or update), same as every other field on this
 * screen. For a brand-new specialist (`isNew`), the photo can be uploaded
 * before the specialist itself exists — upload only needs a salon id, not
 * a specialist id — matching how logo/cover upload also precedes their own
 * "assign" step.
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
    val isNew = specialistId == ManagerDestinations.NEW_SPECIALIST_ID
    val existing = remember(specialistId) {
        if (isNew) null else ManagerRepositories.specialists.getById(specialistId)
    }

    var name by remember(specialistId) { mutableStateOf(existing?.name.orEmpty()) }
    var photoUrl by remember(specialistId) { mutableStateOf(existing?.photoUrl) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val salonId = ManagerRepositories.salonId
        if (salonId == null) {
            errorMessage = "سالن فعال یافت نشد"
            return@rememberLauncherForActivityResult
        }
        errorMessage = null
        isUploadingAvatar = true
        scope.launch {
            val compressed = withContext(Dispatchers.Default) {
                decodeResizeAndCompress(uri, context, maxDimension = AVATAR_MAX_DIMENSION)
            }
            if (compressed == null) {
                isUploadingAvatar = false
                errorMessage = "بارگذاری تصویر ناموفق بود"
                return@launch
            }
            val (bytes, fileName, mimeType) = compressed
            val repository = BackendApiContainerHolder.get(context).managerMediaRepository
            repository.upload(salonId, ManagerMediaType.SPECIALIST_PHOTO, bytes, fileName, mimeType)
                .onSuccess { asset -> photoUrl = asset.url }
                .onFailure { errorMessage = userMessageFor(it) }
            isUploadingAvatar = false
        }
    }

    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            Text(
                text = if (isNew) "متخصص جدید" else "ویرایش متخصص",
                style = RojanTypography.ScreenTitle,
                color = ManagerColors.TextPrimary,
            )

            ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
                ) {
                    StaffAvatarPicker(
                        photoUrl = photoUrl,
                        isUploading = isUploadingAvatar,
                        enabled = !isSubmitting,
                        onChangeClick = {
                            avatarPicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    )

                    StaffNameField(
                        value = name,
                        onValueChange = { name = it },
                        enabled = !isSubmitting,
                    )
                }
            }

            if (isNew) {
                Text(
                    text = "پس از ایجاد متخصص، می‌توانید نمونه‌کارها را اضافه کنید.",
                    style = RojanTypography.Caption,
                    color = ManagerColors.TextSecondary,
                )
            } else {
                ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                    ManagerTargetedMediaGallery(
                        title = "نمونه‌کارها",
                        salonId = ManagerRepositories.salonId ?: "",
                        mediaType = ManagerMediaType.PORTFOLIO,
                        targetId = specialistId,
                        repository = BackendApiContainerHolder.get(context).managerMediaRepository,
                        modifier = Modifier.padding(RojanDimens.SpaceMD),
                    )
                }
            }

            if (errorMessage != null) {
                Text(text = errorMessage.orEmpty(), style = RojanTypography.Caption, color = RojanErrorText)
            }

            ManagerPrimaryButton(
                text = if (isNew) "ایجاد متخصص" else "ذخیره تغییرات",
                enabled = !isSubmitting && !isUploadingAvatar && name.isNotBlank(),
                onClick = {
                    errorMessage = null
                    isSubmitting = true
                    scope.launch {
                        val result = if (isNew) {
                            ManagerRepositories.specialists.create(
                                Specialist(
                                    id = "",
                                    name = name.trim(),
                                    skills = emptyList(),
                                    workingHours = "—",
                                    commissionRate = 0.0,
                                    active = true,
                                    photoUrl = photoUrl,
                                ),
                            )
                        } else {
                            ManagerRepositories.specialists.update((existing ?: return@launch).copy(name = name.trim(), photoUrl = photoUrl))
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
 * Media Sprint P0: circular avatar preview + change/add button, centered
 * above the name field. Reuses [RojanRemoteImage] directly (same component
 * [ai.rojan.designlab.manager.screens.settings.ManagerSalonMediaScreen]'s
 * `IdentityMediaCard` renders through) rather than
 * [ai.rojan.designlab.ui.components.image.SpecialistAvatar] — that
 * component's `assetRes`-fallback design targets the Customer-facing demo
 * roster's bundled drawables, which have no equivalent here.
 */
@Composable
private fun StaffAvatarPicker(
    photoUrl: String?,
    isUploading: Boolean,
    enabled: Boolean,
    onChangeClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
    ) {
        Box(
            modifier = Modifier
                .size(AVATAR_DISPLAY_SIZE)
                .background(ManagerColors.BaseSecondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            RojanRemoteImage(
                url = photoUrl,
                contentDescription = "عکس متخصص",
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                fallback = { Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = ManagerColors.TextSecondary) },
            )
            if (isUploading) {
                Box(modifier = Modifier.fillMaxSize().background(ManagerColors.BaseDeep.copy(alpha = 0.55f), CircleShape), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ManagerColors.Turquoise)
                }
            }
        }

        TextButton(enabled = enabled && !isUploading, onClick = onChangeClick) {
            Text(
                text = if (isUploading) "در حال آپلود..." else if (photoUrl == null) "افزودن عکس" else "تغییر عکس",
                color = ManagerColors.Turquoise,
            )
        }
    }
}

private val AVATAR_DISPLAY_SIZE = 96.dp
private const val AVATAR_MAX_DIMENSION = 512

/** Minimal, self-contained Manager-themed text field — same styling as [ai.rojan.designlab.manager.screens.auth.ManagerOtpAuthScreen]'s own local `ManagerTextField` (no shared Manager text-field component exists yet to reuse). */
@Composable
private fun StaffNameField(value: String, onValueChange: (String) -> Unit, enabled: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("نام متخصص") },
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
