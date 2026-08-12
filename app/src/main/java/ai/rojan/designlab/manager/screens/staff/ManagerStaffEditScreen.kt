package ai.rojan.designlab.manager.screens.staff

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.domain.specialist.Specialist
import ai.rojan.designlab.manager.navigation.ManagerDestinations
import ai.rojan.designlab.presentation.common.userMessageFor
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTheme
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.text.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch

/**
 * Manager App workspace — one screen for both creating and editing a
 * [Specialist] (Manager Operational Foundation, Phase 6 Step 2).
 * [specialistId] == [ManagerDestinations.NEW_SPECIALIST_ID] means create;
 * any other value loads that specialist from
 * [ManagerRepositories.specialists] for editing — same "route param
 * doubles as a mode sentinel" shape [ManagerDestinations.STAFF_EDIT]
 * documents.
 *
 * Only [Specialist.name] is editable — `bio`/`photoUrl` exist on the
 * backend's create/update DTOs but aren't part of this domain model yet
 * (see [ai.rojan.designlab.manager.data.BackendSpecialistRepository]'s own
 * doc comment on why `skills`/`workingHours`/`commissionRate` are already
 * disclosed placeholders); not adding fields here that wouldn't actually
 * persist anything real.
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
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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
                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                ) {
                    StaffNameField(
                        value = name,
                        onValueChange = { name = it },
                        enabled = !isSubmitting,
                    )
                }
            }

            if (errorMessage != null) {
                Text(text = errorMessage.orEmpty(), style = RojanTypography.Caption, color = RojanErrorText)
            }

            ManagerPrimaryButton(
                text = if (isNew) "ایجاد متخصص" else "ذخیره تغییرات",
                enabled = !isSubmitting && name.isNotBlank(),
                onClick = {
                    errorMessage = null
                    isSubmitting = true
                    scope.launch {
                        val result = if (isNew) {
                            ManagerRepositories.specialists.create(
                                Specialist(id = "", name = name.trim(), skills = emptyList(), workingHours = "—", commissionRate = 0.0, active = true),
                            )
                        } else {
                            ManagerRepositories.specialists.update((existing ?: return@launch).copy(name = name.trim()))
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
