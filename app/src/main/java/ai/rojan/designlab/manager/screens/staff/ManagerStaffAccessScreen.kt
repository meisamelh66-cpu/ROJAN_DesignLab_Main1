package ai.rojan.designlab.manager.screens.staff

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.domain.membership.SalonMember
import ai.rojan.designlab.manager.domain.membership.SalonMemberRole
import ai.rojan.designlab.manager.domain.membership.StaffCandidate
import ai.rojan.designlab.manager.presentation.staff.AddStaffUiState
import ai.rojan.designlab.manager.presentation.staff.ManagerStaffAccessViewModel
import ai.rojan.designlab.manager.presentation.staff.StaffAccessUiState
import ai.rojan.designlab.ui.components.cards.PremiumCardShell
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.text.withDirectionFor
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Staff Access Phase 1 - the real Staff Access screen: existing-staff list (name/phone/role/
 * active state), "افزودن کارمند" (Add Employee) phone-lookup-confirm-role flow, and per-member
 * change-role/disable/re-enable actions. Deliberately separate from [ai.rojan.designlab.manager.screens.staff.ManagerStaffScreen]'s
 * "افزودن متخصص" (a catalog/scheduling concept with no account or role) and from the existing
 * "دعوت پذیرش" invite-token row (kept as-is, not superseded by this phase).
 */
@Composable
fun ManagerStaffAccessScreen(
    viewModel: ManagerStaffAccessViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val addState by viewModel.addState.collectAsStateWithLifecycle()

    ManagerScaffold(onBackClick = onBackClick) {
        if (addState !is AddStaffUiState.Hidden) {
            AddStaffFlow(
                addState = addState,
                onPhoneChange = viewModel::onPhoneInputChange,
                onLookupClick = viewModel::lookupPhone,
                onRoleSelected = viewModel::onRoleSelected,
                onConfirmClick = viewModel::confirmAssign,
                onRetryClick = viewModel::retryAdd,
                onCancelClick = viewModel::cancelAddStaff,
            )
        } else {
            StaffListContent(
                state = state,
                onAddClick = viewModel::startAddStaff,
                onToggleActive = viewModel::setMemberActive,
                onRoleChange = viewModel::changeRole,
            )
        }
    }
}

@Composable
private fun StaffListContent(
    state: StaffAccessUiState,
    onAddClick: () -> Unit,
    onToggleActive: (userId: String, active: Boolean) -> Unit,
    onRoleChange: (userId: String, role: SalonMemberRole) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        item {
            RtlSectionHeader(
                text = "دسترسی کارکنان",
                style = RojanTypography.ScreenTitle,
                color = ManagerColors.TextPrimary,
                horizontalPadding = 0.dp,
            )
        }

        item { AddEmployeeRow(onClick = onAddClick) }

        when (state) {
            is StaffAccessUiState.Loading -> item { LoadingNotice() }
            is StaffAccessUiState.Error -> item { StaffAccessErrorNotice(message = state.message) }
            is StaffAccessUiState.Loaded -> {
                if (state.members.isEmpty()) {
                    item { EmptyMembersNotice() }
                } else {
                    items(state.members, key = { it.id }) { member ->
                        StaffMemberCard(
                            member = member,
                            onToggleActive = { onToggleActive(member.userId, !member.active) },
                            onRoleChange = { role -> onRoleChange(member.userId, role) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEmployeeRow(onClick: () -> Unit) {
    ManagerGlassSurface(
        modifier = Modifier.fillMaxWidth().rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceSM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            RojanIconContainer(
                imageVector = Icons.Filled.PersonAdd,
                contentDescription = "افزودن کارمند",
                size = RojanIconSize.Medium,
                tint = ManagerColors.Gold,
            )
            Text(text = "افزودن کارمند", style = RojanTypography.Body, color = ManagerColors.TextPrimary)
        }
    }
}

@Composable
private fun StaffMemberCard(
    member: SalonMember,
    onToggleActive: () -> Unit,
    onRoleChange: (SalonMemberRole) -> Unit,
) {
    PremiumCardShell(shape = RojanShapes.Small) {
        Column(verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
            ) {
                RojanIconContainer(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    size = RojanIconSize.Medium,
                    tint = ManagerColors.Turquoise,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.fullName.ifBlank { "بدون نام" },
                        style = RojanTypography.Body,
                        color = ManagerColors.TextPrimary,
                    )
                    if (member.phoneNumber != null) {
                        Text(text = member.phoneNumber, style = RojanTypography.Caption, color = ManagerColors.TextSecondary)
                    }
                }
                StatusChip(active = member.active)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
            ) {
                RoleToggleButton(
                    label = "مدیر",
                    selected = member.role == SalonMemberRole.MANAGER,
                    onClick = { onRoleChange(SalonMemberRole.MANAGER) },
                )
                RoleToggleButton(
                    label = "پذیرش",
                    selected = member.role == SalonMemberRole.RECEPTIONIST,
                    onClick = { onRoleChange(SalonMemberRole.RECEPTIONIST) },
                )
                Box(modifier = Modifier.weight(1f))
                ActiveToggleButton(active = member.active, onClick = onToggleActive)
            }
        }
    }
}

@Composable
private fun StatusChip(active: Boolean) {
    val (label, color) = if (active) "فعال" to ManagerColors.Turquoise else "غیرفعال" to ManagerColors.TextSecondary
    Text(text = label, style = RojanTypography.Caption, color = color)
}

@Composable
private fun RoleToggleButton(label: String, selected: Boolean, onClick: () -> Unit) {
    ManagerGlassSurface(
        modifier = Modifier.rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
        fillAlpha = if (selected) 0.22f else 0.08f,
    ) {
        Text(
            text = label,
            style = RojanTypography.Caption,
            color = if (selected) ManagerColors.Gold else ManagerColors.TextSecondary,
            modifier = Modifier.padding(horizontal = RojanDimens.SpaceSM, vertical = RojanDimens.SpaceXS),
        )
    }
}

@Composable
private fun ActiveToggleButton(active: Boolean, onClick: () -> Unit) {
    ManagerGlassSurface(
        modifier = Modifier.rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
    ) {
        Text(
            text = if (active) "غیرفعال‌سازی" else "فعال‌سازی",
            style = RojanTypography.Caption,
            color = if (active) ManagerColors.TextSecondary else ManagerColors.Turquoise,
            modifier = Modifier.padding(horizontal = RojanDimens.SpaceSM, vertical = RojanDimens.SpaceXS),
        )
    }
}

@Composable
private fun AddStaffFlow(
    addState: AddStaffUiState,
    onPhoneChange: (String) -> Unit,
    onLookupClick: () -> Unit,
    onRoleSelected: (SalonMemberRole) -> Unit,
    onConfirmClick: () -> Unit,
    onRetryClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(RojanDimens.SpaceMD),
        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RtlSectionHeader(
                text = "افزودن کارمند",
                style = RojanTypography.ScreenTitle,
                color = ManagerColors.TextPrimary,
                horizontalPadding = 0.dp,
            )
            RojanIconContainer(
                imageVector = Icons.Filled.Close,
                contentDescription = "انصراف",
                size = RojanIconSize.Medium,
                tint = ManagerColors.TextSecondary,
                modifier = Modifier.rojanPressable(onClick = onCancelClick),
            )
        }

        when (addState) {
            is AddStaffUiState.EnteringPhone -> PhoneEntryStep(
                phone = addState.phone,
                error = addState.error,
                onPhoneChange = onPhoneChange,
                onLookupClick = onLookupClick,
            )
            is AddStaffUiState.Looking -> LoadingNotice()
            is AddStaffUiState.Confirming -> ConfirmStep(
                candidate = addState.candidate,
                selectedRole = addState.selectedRole,
                onRoleSelected = onRoleSelected,
                onConfirmClick = onConfirmClick,
            )
            is AddStaffUiState.Assigning -> LoadingNotice()
            is AddStaffUiState.Error -> StaffAccessErrorNotice(message = addState.message, onRetryClick = onRetryClick)
            is AddStaffUiState.Hidden -> Unit
        }
    }
}

@Composable
private fun PhoneEntryStep(phone: String, error: String?, onPhoneChange: (String) -> Unit, onLookupClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
        Text(
            text = "شماره موبایل کارمندی که می‌خواهید به سالن دسترسی دهید را وارد کنید.",
            style = RojanTypography.Body,
            color = ManagerColors.TextSecondary,
        )
        ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
            Box(modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD)) {
                BasicTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = RojanTypography.Body.copy(color = ManagerColors.TextPrimary).withDirectionFor(phone),
                    cursorBrush = SolidColor(ManagerColors.Turquoise),
                    decorationBox = { innerTextField ->
                        if (phone.isEmpty()) {
                            Text(text = "09xxxxxxxxx", style = RojanTypography.Body, color = ManagerColors.TextSecondary)
                        }
                        innerTextField()
                    },
                )
            }
        }
        if (error != null) {
            Text(text = error, style = RojanTypography.Caption, color = ManagerColors.Gold)
        }
        RoleToggleButton(label = "بررسی شماره", selected = true, onClick = onLookupClick)
    }
}

@Composable
private fun ConfirmStep(
    candidate: StaffCandidate,
    selectedRole: SalonMemberRole,
    onRoleSelected: (SalonMemberRole) -> Unit,
    onConfirmClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD)) {
        PremiumCardShell(shape = RojanShapes.Small) {
            Column(verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS)) {
                Text(text = candidate.fullName, style = RojanTypography.Body, color = ManagerColors.TextPrimary)
                Text(text = candidate.phoneNumber, style = RojanTypography.Caption, color = ManagerColors.TextSecondary)
            }
        }
        Text(text = "نقش را انتخاب کنید:", style = RojanTypography.Body, color = ManagerColors.TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
            RoleToggleButton(label = "مدیر", selected = selectedRole == SalonMemberRole.MANAGER, onClick = { onRoleSelected(SalonMemberRole.MANAGER) })
            RoleToggleButton(label = "پذیرش", selected = selectedRole == SalonMemberRole.RECEPTIONIST, onClick = { onRoleSelected(SalonMemberRole.RECEPTIONIST) })
        }
        RoleToggleButton(label = "تأیید و فعال‌سازی دسترسی", selected = true, onClick = onConfirmClick)
    }
}

@Composable
private fun LoadingNotice() {
    Box(modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceLG), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ManagerColors.Turquoise)
    }
}

@Composable
private fun StaffAccessErrorNotice(message: String, onRetryClick: (() -> Unit)? = null) {
    ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceLG),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            Text(text = message, style = RojanTypography.Body, color = ManagerColors.TextPrimary, textAlign = TextAlign.Center)
            if (onRetryClick != null) {
                RoleToggleButton(label = "تلاش مجدد", selected = true, onClick = onRetryClick)
            }
        }
    }
}

@Composable
private fun EmptyMembersNotice() {
    ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
        Text(
            text = "هنوز کارمندی به این سالن دسترسی ندارد.",
            style = RojanTypography.Body,
            color = ManagerColors.TextSecondary,
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceLG),
            textAlign = TextAlign.Center,
        )
    }
}
