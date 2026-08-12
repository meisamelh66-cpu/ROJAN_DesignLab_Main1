package ai.rojan.designlab.manager.screens.staff

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.domain.specialist.Specialist
import ai.rojan.designlab.manager.screens.customers.TagChip
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.text.withDirectionFor
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTheme
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Manager App workspace — Specialist Roster MVP: list + search, same shape
 * as [ai.rojan.designlab.manager.screens.services.ManagerServicesScreen]/
 * [ai.rojan.designlab.manager.screens.customers.ManagerCustomersListScreen]
 * (Manager Operational Foundation, Phase 6 Step 2). Unlike Services'
 * MVP, this one also ships create/edit/deactivate (via [onAddClick]/
 * [onSpecialistClick] routing to
 * [ai.rojan.designlab.manager.screens.staff.ManagerStaffEditScreen]) —
 * explicit scope for this step.
 *
 * Reads [ManagerRepositories.specialists] directly, no re-[ManagerRepositories.initialize]
 * call — only reachable via the Dashboard, which already populated the
 * cache, same assumption every other post-Dashboard Manager screen makes.
 */
@Composable
fun ManagerStaffScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onAddClick: () -> Unit = {},
    onSpecialistClick: (String) -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    val filteredSpecialists = remember(query) {
        val all = ManagerRepositories.specialists.getAll()
        if (query.isBlank()) all else all.filter { it.name.contains(query, ignoreCase = true) }
    }

    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item {
                RtlSectionHeader(
                    text = "کارکنان",
                    style = RojanTypography.ScreenTitle,
                    color = ManagerColors.TextPrimary,
                    horizontalPadding = 0.dp,
                )
            }

            item { AddSpecialistRow(onClick = onAddClick) }

            item {
                StaffSearchField(
                    query = query,
                    onQueryChange = { query = it },
                )
            }

            if (filteredSpecialists.isEmpty()) {
                item { EmptyStaffNotice() }
            } else {
                items(filteredSpecialists) { specialist ->
                    SpecialistCard(
                        specialist = specialist,
                        onClick = { onSpecialistClick(specialist.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AddSpecialistRow(onClick: () -> Unit) {
    ManagerGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceSM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            RojanIconContainer(
                imageVector = Icons.Filled.PersonAdd,
                contentDescription = "افزودن متخصص",
                size = RojanIconSize.Medium,
                tint = ManagerColors.Gold,
            )
            Text(text = "افزودن متخصص", style = RojanTypography.Body, color = ManagerColors.TextPrimary)
        }
    }
}

@Composable
private fun StaffSearchField(query: String, onQueryChange: (String) -> Unit) {
    ManagerGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RojanShapes.Small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceSM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            RojanIconContainer(
                imageVector = Icons.Filled.Search,
                contentDescription = "جستجو",
                size = RojanIconSize.Medium,
                tint = ManagerColors.Turquoise,
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = RojanTypography.Body.copy(color = ManagerColors.TextPrimary).withDirectionFor(query),
                    cursorBrush = SolidColor(ManagerColors.Turquoise),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = "جستجوی نام متخصص...",
                                style = RojanTypography.Body,
                                color = ManagerColors.TextSecondary,
                            )
                        }
                        innerTextField()
                    },
                )
            }
        }
    }
}

@Composable
private fun SpecialistCard(specialist: Specialist, onClick: () -> Unit) {
    ManagerGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
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
                Text(text = specialist.name, style = RojanTypography.Body, color = ManagerColors.TextPrimary)
            }

            TagChip(text = if (specialist.active) "فعال" else "غیرفعال")
        }
    }
}

@Composable
private fun EmptyStaffNotice() {
    ManagerGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RojanShapes.Small,
    ) {
        Text(
            text = "متخصصی برای این سالن ثبت نشده است.",
            style = RojanTypography.Body,
            color = ManagerColors.TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceLG),
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
)
@Composable
private fun ManagerStaffScreenPreview() {
    RojanTheme {
        ManagerStaffScreen()
    }
}
