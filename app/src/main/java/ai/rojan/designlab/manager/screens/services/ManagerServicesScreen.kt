package ai.rojan.designlab.manager.screens.services

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.data.formatDurationMinutes
import ai.rojan.designlab.manager.data.formatTomanPrice
import ai.rojan.designlab.manager.domain.service.Service
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ContentCut
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
 * Manager App workspace — Services: list + search + create/edit/deactivate
 * (Manager Operational Foundation, Phase 6 Step 3 — create/edit/deactivate
 * complete the CRUD this screen's Step 1 MVP deliberately deferred, via
 * [onAddClick]/[onServiceClick] routing to
 * [ai.rojan.designlab.manager.screens.services.ManagerServiceEditScreen]).
 *
 * Reads [ManagerRepositories.services] directly, no re-[ManagerRepositories.initialize]
 * call — this screen is only reachable via the Dashboard's Quick Actions,
 * which has already populated the cache, exactly the same assumption
 * `ManagerCustomersListScreen` makes.
 */
@Composable
fun ManagerServicesScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onAddClick: () -> Unit = {},
    onServiceClick: (String) -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    val filteredServices = remember(query) {
        val all = ManagerRepositories.services.getAll()
        if (query.isBlank()) {
            all
        } else {
            all.filter { it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true) }
        }
    }

    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item {
                RtlSectionHeader(
                    text = "خدمات",
                    style = RojanTypography.ScreenTitle,
                    color = ManagerColors.TextPrimary,
                    horizontalPadding = 0.dp,
                )
            }

            item { AddServiceRow(onClick = onAddClick) }

            item {
                ServiceSearchField(
                    query = query,
                    onQueryChange = { query = it },
                )
            }

            if (filteredServices.isEmpty()) {
                item { EmptyServicesNotice() }
            } else {
                items(filteredServices) { service ->
                    ServiceCard(service = service, onClick = { onServiceClick(service.id) })
                }
            }
        }
    }
}

@Composable
private fun AddServiceRow(onClick: () -> Unit) {
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
                imageVector = Icons.Filled.AddCircle,
                contentDescription = "افزودن خدمت",
                size = RojanIconSize.Medium,
                tint = ManagerColors.Gold,
            )
            Text(text = "افزودن خدمت", style = RojanTypography.Body, color = ManagerColors.TextPrimary)
        }
    }
}

@Composable
private fun ServiceSearchField(query: String, onQueryChange: (String) -> Unit) {
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
                                text = "جستجوی نام یا دسته‌بندی...",
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
private fun ServiceCard(service: Service, onClick: () -> Unit) {
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
                imageVector = Icons.Filled.ContentCut,
                contentDescription = null,
                size = RojanIconSize.Medium,
                tint = ManagerColors.Turquoise,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(text = service.name, style = RojanTypography.Body, color = ManagerColors.TextPrimary)
                Text(
                    text = "${service.category} · ${formatDurationMinutes(service.durationMinutes)} · ${formatTomanPrice(service.price)}",
                    style = RojanTypography.Caption,
                    color = ManagerColors.TextSecondary,
                )
            }

            TagChip(text = if (service.active) "فعال" else "غیرفعال")
        }
    }
}

@Composable
private fun EmptyServicesNotice() {
    ManagerGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RojanShapes.Small,
    ) {
        Text(
            text = "خدمتی برای این سالن ثبت نشده است.",
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
private fun ManagerServicesScreenPreview() {
    RojanTheme {
        ManagerServicesScreen()
    }
}
