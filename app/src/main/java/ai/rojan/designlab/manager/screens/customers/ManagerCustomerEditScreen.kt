package ai.rojan.designlab.manager.screens.customers

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerGlassTheme
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.domain.customer.CustomerTag
import ai.rojan.designlab.manager.domain.customer.displayLabel
import ai.rojan.designlab.presentation.common.userMessageFor
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTheme
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch

/**
 * Manager App workspace — Customer Edit (Phase 9 Step 1). Edit-only, no
 * create mode — a customer always already exists by the time this screen
 * is reached (from [ManagerCustomerProfileScreen]'s edit entry point),
 * unlike [ai.rojan.designlab.manager.screens.staff.ManagerStaffEditScreen]/
 * [ai.rojan.designlab.manager.screens.services.ManagerServiceEditScreen]'s
 * shared create/edit shape.
 *
 * **Exactly three fields are editable, and only these** —
 * [ai.rojan.designlab.manager.domain.customer.ManagerCustomer.name]/`.phone`/`.tag`
 * — because [ai.rojan.designlab.manager.data.BackendCustomerRepository.update]
 * already only ever sends `fullName`/`phoneNumber`/`status` to the real
 * `PATCH .../customers/{customerId}` endpoint (verified by direct
 * inspection, Phase 9 Step 1's own audit). The backend's
 * `UpdateCustomerRequestDto` also has `email`/`company`, but
 * [ai.rojan.designlab.manager.domain.customer.ManagerCustomer] never
 * carried those fields - not added here just because the DTO has them,
 * same discipline [ManagerStaffEditScreen] already applies to
 * `bio`/`photoUrl`. `notes`/`loyaltyScore`/`lastVisit`/`totalVisits` stay
 * unexposed too: no note-creation endpoint exists, `loyaltyScore` has no
 * backend equivalent (see [ai.rojan.designlab.manager.data.BackendCustomerRepository]'s
 * own doc comment), and the visit stats are derived, not directly
 * settable.
 *
 * Phone is not required - an empty value is sent as `null`
 * ([ai.rojan.designlab.manager.data.BackendCustomerRepository.update]'s
 * own `.ifBlank { null }`), which the backend's partial-update semantics
 * treat as "leave unchanged," not "clear."
 *
 * [onSaved] fires after a successful save; the caller returns to the
 * customer profile.
 */
@Composable
fun ManagerCustomerEditScreen(
    modifier: Modifier = Modifier,
    customerId: String,
    onBackClick: (() -> Unit)? = null,
    onSaved: () -> Unit = {},
) {
    val existing = remember(customerId) { ManagerRepositories.customers.getById(customerId) }

    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        if (existing == null) {
            return@ManagerScaffold
        }

        var name by remember(customerId) { mutableStateOf(existing.name) }
        var phone by remember(customerId) { mutableStateOf(existing.phone) }
        var selectedTag by remember(customerId) { mutableStateOf(existing.tag) }
        var isSubmitting by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        Column(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            Text(
                text = "ویرایش مشتری",
                style = RojanTypography.ScreenTitle,
                color = ManagerColors.TextPrimary,
            )

            ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                ) {
                    CustomerEditTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "نام مشتری",
                        keyboardType = KeyboardType.Text,
                        enabled = !isSubmitting,
                    )
                    CustomerEditTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = "شماره تماس",
                        keyboardType = KeyboardType.Phone,
                        enabled = !isSubmitting,
                    )

                    Text(text = "دسته‌بندی", style = RojanTypography.Caption, color = ManagerColors.TextSecondary)
                    CustomerTagPickerRow(
                        selectedTag = selectedTag,
                        onTagSelected = { selectedTag = it },
                    )
                }
            }

            if (errorMessage != null) {
                Text(text = errorMessage.orEmpty(), style = RojanTypography.Caption, color = RojanErrorText)
            }

            ManagerPrimaryButton(
                text = "ذخیره تغییرات",
                enabled = !isSubmitting && name.isNotBlank(),
                onClick = {
                    errorMessage = null
                    isSubmitting = true
                    scope.launch {
                        ManagerRepositories.customers
                            .update(existing.copy(name = name.trim(), phone = phone.trim(), tag = selectedTag))
                            .onSuccess { isSubmitting = false; onSaved() }
                            .onFailure { isSubmitting = false; errorMessage = userMessageFor(it) }
                    }
                },
            )
        }
    }
}

@Composable
private fun CustomerTagPickerRow(selectedTag: CustomerTag, onTagSelected: (CustomerTag) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
        items(CustomerTag.entries) { tag ->
            val selected = tag == selectedTag
            ManagerGlassSurface(
                modifier = Modifier.rojanPressable(onClick = { onTagSelected(tag) }),
                shape = RojanShapes.Small,
                fillAlpha = if (selected) ManagerGlassTheme.FillAlpha else ManagerGlassTheme.FillAlpha * 0.5f,
                borderAlpha = if (selected) ManagerGlassTheme.BorderAlpha else ManagerGlassTheme.BorderAlpha * 0.4f,
            ) {
                Text(
                    text = tag.displayLabel,
                    style = RojanTypography.Caption,
                    color = if (selected) ManagerColors.TextPrimary else ManagerColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceSM),
                )
            }
        }
    }
}

/** Minimal, self-contained Manager-themed text field — same styling as [ai.rojan.designlab.manager.screens.staff.ManagerStaffEditScreen]'s own local field (no shared Manager text-field component exists yet to reuse). */
@Composable
private fun CustomerEditTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    enabled: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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
private fun ManagerCustomerEditScreenPreview() {
    RojanTheme {
        ManagerCustomerEditScreen(customerId = "c1")
    }
}
