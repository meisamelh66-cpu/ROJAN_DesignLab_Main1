package ai.rojan.designlab.manager.screens.services

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerGlassTheme
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.components.ManagerTargetedMediaGallery
import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.domain.media.ManagerMediaType
import ai.rojan.designlab.manager.domain.service.Service
import ai.rojan.designlab.manager.navigation.ManagerDestinations
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch

/**
 * Manager App workspace — one screen for both creating and editing a
 * [Service] (Manager Operational Foundation, Phase 6 Step 3), same
 * "route param doubles as a mode sentinel" shape as
 * [ai.rojan.designlab.manager.screens.staff.ManagerStaffEditScreen].
 * [serviceId] == [ManagerDestinations.NEW_SERVICE_ID] means create; any
 * other value loads that service from [ManagerRepositories.services] for
 * editing.
 *
 * **Category is only selectable at create time.** [ai.rojan.designlab.manager.data.BackendServiceRepository.update]
 * resolves the write's `categoryId` from the service's *original*
 * category (`categoryIdByServiceId`), not from whatever
 * [Service.category] the caller passes in — the backend's update endpoint
 * has no field or path shape for moving a service to a different
 * category. Showing a category picker in edit mode would silently do
 * nothing, so edit mode shows the category as read-only text instead of
 * inventing a capability that doesn't exist.
 *
 * [description] (on the create/update DTOs) has no field in the [Service]
 * domain model and isn't added here, same "don't add a field that can't
 * actually round-trip" discipline [ManagerStaffEditScreen] already
 * applies to `bio`/`photoUrl`.
 *
 * [onSaved] fires after a successful create, update, or deactivate — in
 * every case the caller returns to the services list.
 */
@Composable
fun ManagerServiceEditScreen(
    modifier: Modifier = Modifier,
    serviceId: String = ManagerDestinations.NEW_SERVICE_ID,
    onBackClick: (() -> Unit)? = null,
    onSaved: () -> Unit = {},
) {
    val isNew = serviceId == ManagerDestinations.NEW_SERVICE_ID
    val existing = remember(serviceId) {
        if (isNew) null else ManagerRepositories.services.getById(serviceId)
    }
    val categoryNames = remember { ManagerRepositories.services.getCategoryNames() }

    var name by remember(serviceId) { mutableStateOf(existing?.name.orEmpty()) }
    var selectedCategory by remember(serviceId) { mutableStateOf(existing?.category ?: categoryNames.firstOrNull().orEmpty()) }
    var priceText by remember(serviceId) { mutableStateOf(existing?.price?.toString().orEmpty()) }
    var durationText by remember(serviceId) { mutableStateOf(existing?.durationMinutes?.toString().orEmpty()) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val price = priceText.toLongOrNull()
    val duration = durationText.toIntOrNull()
    val canSave = !isSubmitting && name.isNotBlank() && selectedCategory.isNotBlank() && price != null && duration != null

    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            Text(
                text = if (isNew) "خدمت جدید" else "ویرایش خدمت",
                style = RojanTypography.ScreenTitle,
                color = ManagerColors.TextPrimary,
            )

            ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                ) {
                    ServiceTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "نام خدمت",
                        keyboardType = KeyboardType.Text,
                        enabled = !isSubmitting,
                    )
                    ServiceTextField(
                        value = priceText,
                        onValueChange = { priceText = it.filter(Char::isDigit) },
                        label = "قیمت (تومان)",
                        keyboardType = KeyboardType.Number,
                        enabled = !isSubmitting,
                    )
                    ServiceTextField(
                        value = durationText,
                        onValueChange = { durationText = it.filter(Char::isDigit) },
                        label = "مدت زمان (دقیقه)",
                        keyboardType = KeyboardType.Number,
                        enabled = !isSubmitting,
                    )

                    Text(text = "دسته‌بندی", style = RojanTypography.Caption, color = ManagerColors.TextSecondary)
                    if (isNew) {
                        if (categoryNames.isEmpty()) {
                            Text(
                                text = "هیچ دسته‌بندی‌ای برای این سالن ثبت نشده است.",
                                style = RojanTypography.Caption,
                                color = RojanErrorText,
                            )
                        } else {
                            CategoryChipRow(
                                categoryNames = categoryNames,
                                selectedCategory = selectedCategory,
                                onCategorySelected = { selectedCategory = it },
                            )
                        }
                    } else {
                        Text(text = selectedCategory, style = RojanTypography.Body, color = ManagerColors.TextPrimary)
                    }
                }
            }

            if (isNew) {
                Text(
                    text = "پس از ایجاد خدمت، می‌توانید تصاویر آن را اضافه کنید.",
                    style = RojanTypography.Caption,
                    color = ManagerColors.TextSecondary,
                )
            } else {
                ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
                    ManagerTargetedMediaGallery(
                        title = "تصاویر خدمت",
                        salonId = ManagerRepositories.salonId ?: "",
                        mediaType = ManagerMediaType.SERVICE_IMAGE,
                        targetId = serviceId,
                        repository = BackendApiContainerHolder.get(context).managerMediaRepository,
                        modifier = Modifier.padding(RojanDimens.SpaceMD),
                    )
                }
            }

            if (errorMessage != null) {
                Text(text = errorMessage.orEmpty(), style = RojanTypography.Caption, color = RojanErrorText)
            }

            ManagerPrimaryButton(
                text = if (isNew) "ایجاد خدمت" else "ذخیره تغییرات",
                enabled = canSave,
                onClick = {
                    val validPrice = price ?: return@ManagerPrimaryButton
                    val validDuration = duration ?: return@ManagerPrimaryButton
                    errorMessage = null
                    isSubmitting = true
                    scope.launch {
                        val result = if (isNew) {
                            ManagerRepositories.services.create(
                                Service(id = "", name = name.trim(), category = selectedCategory, price = validPrice, durationMinutes = validDuration, active = true),
                            )
                        } else {
                            ManagerRepositories.services.update(
                                (existing ?: return@launch).copy(name = name.trim(), price = validPrice, durationMinutes = validDuration),
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
                            ManagerRepositories.services.delete(serviceId)
                                .onSuccess { isSubmitting = false; onSaved() }
                                .onFailure { isSubmitting = false; errorMessage = userMessageFor(it) }
                        }
                    },
                ) {
                    Text("غیرفعال‌سازی این خدمت", color = RojanErrorText)
                }
            }
        }
    }
}

@Composable
private fun CategoryChipRow(
    categoryNames: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
        items(categoryNames) { category ->
            val selected = category == selectedCategory
            ManagerGlassSurface(
                modifier = Modifier.rojanPressable(onClick = { onCategorySelected(category) }),
                shape = RojanShapes.Small,
                fillAlpha = if (selected) ManagerGlassTheme.FillAlpha else ManagerGlassTheme.FillAlpha * 0.5f,
                borderAlpha = if (selected) ManagerGlassTheme.BorderAlpha else ManagerGlassTheme.BorderAlpha * 0.4f,
            ) {
                Text(
                    text = category,
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
private fun ServiceTextField(
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
private fun ManagerServiceEditScreenPreview() {
    RojanTheme {
        ManagerServiceEditScreen()
    }
}
