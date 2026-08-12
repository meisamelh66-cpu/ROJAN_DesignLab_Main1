package ai.rojan.designlab.manager.screens.customers

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerGlassTheme
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.domain.customer.CustomerTag
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import ai.rojan.designlab.manager.domain.customer.displayLabel
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
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
 * Manager App workspace — Customers MVP: list + search. Additive-only:
 * does not modify [ai.rojan.designlab.manager.screens.dashboard.ManagerDashboardScreen]
 * or [ai.rojan.designlab.manager.screens.calendar.ManagerCalendarScreen].
 *
 * ROJAN AI Manager Visual Theme Implementation: re-themed for the dark
 * luxury background ([ManagerScaffold]/[ManagerGlassSurface]) —
 * content/data/navigation unchanged.
 *
 * Search is a real, working local filter over the cache
 * [ManagerRepositories.customers] syncs from the real backend Customer
 * CRM API (Phase 2, M2). The list row deliberately doesn't show a
 * per-customer "last visit" — the bulk listing endpoint doesn't include
 * it, and fetching it per row would be an N+1 call (see
 * [ai.rojan.designlab.manager.data.BackendCustomerRepository]'s own doc
 * comment); it's shown on the profile screen instead, where a single
 * extra call for one customer is the normal case.
 *
 * CRM Foundation, Phase 6 Step 5: `selectedTag` adds a [CustomerTag]
 * filter on top of [query] — both local, over the same already-synced
 * cache `search()` already reads, no extra network call.
 *
 * AI Insight Presentation Layer, Phase 7 Step 4: [initialTagFilter] seeds
 * that same filter state from the caller (e.g. the Dashboard's inactive-
 * customer summary linking straight to the pre-filtered list) — a manager
 * can still change or clear it afterward exactly as before, since it's
 * only the initial value, not a locked/controlled one.
 */
@Composable
fun ManagerCustomersListScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onCustomerClick: (String) -> Unit = {},
    initialTagFilter: CustomerTag? = null,
) {
    var query by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf(initialTagFilter) }
    val filteredCustomers = remember(query, selectedTag) {
        ManagerRepositories.customers.search(query).filter { customer ->
            selectedTag == null || customer.tag == selectedTag
        }
    }

    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item {
                RtlSectionHeader(
                    text = "مشتریان",
                    style = RojanTypography.ScreenTitle,
                    color = ManagerColors.TextPrimary,
                    horizontalPadding = 0.dp,
                )
            }

            item {
                CustomerSearchField(
                    query = query,
                    onQueryChange = { query = it },
                )
            }

            item {
                CustomerTagFilterRow(
                    selectedTag = selectedTag,
                    onTagSelected = { selectedTag = it },
                )
            }

            if (filteredCustomers.isEmpty()) {
                item { EmptyCustomersNotice() }
            } else {
                items(filteredCustomers) { customer ->
                    CustomerCard(
                        customer = customer,
                        onClick = { onCustomerClick(customer.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerSearchField(query: String, onQueryChange: (String) -> Unit) {
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
                                text = "جستجوی نام یا شماره تماس...",
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

/** CRM Foundation, Phase 6 Step 5 — same selectable-chip-row pattern as [ai.rojan.designlab.manager.screens.calendar.ManagerCalendarScreen]'s specialist filter; "همه" (all) plus one chip per real [CustomerTag] value. */
@Composable
private fun CustomerTagFilterRow(selectedTag: CustomerTag?, onTagSelected: (CustomerTag?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM)) {
        item {
            CustomerTagChip(
                label = "همه",
                selected = selectedTag == null,
                onClick = { onTagSelected(null) },
            )
        }
        items(CustomerTag.entries) { tag ->
            CustomerTagChip(
                label = tag.displayLabel,
                selected = selectedTag == tag,
                onClick = { onTagSelected(tag) },
            )
        }
    }
}

@Composable
private fun CustomerTagChip(label: String, selected: Boolean, onClick: () -> Unit) {
    ManagerGlassSurface(
        modifier = Modifier.rojanPressable(onClick = onClick),
        shape = RojanShapes.Small,
        fillAlpha = if (selected) ManagerGlassTheme.FillAlpha else ManagerGlassTheme.FillAlpha * 0.5f,
        borderAlpha = if (selected) ManagerGlassTheme.BorderAlpha else ManagerGlassTheme.BorderAlpha * 0.4f,
    ) {
        Text(
            text = label,
            style = RojanTypography.Caption,
            color = if (selected) ManagerColors.TextPrimary else ManagerColors.TextSecondary,
            modifier = Modifier.padding(horizontal = RojanDimens.SpaceMD, vertical = RojanDimens.SpaceSM),
        )
    }
}

@Composable
private fun CustomerCard(customer: ManagerCustomer, onClick: () -> Unit) {
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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(ManagerColors.Turquoise.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = customer.name.take(1),
                    style = RojanTypography.CardTitle,
                    color = ManagerColors.TurquoiseLight,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = customer.name, style = RojanTypography.Body, color = ManagerColors.TextPrimary)
                Text(text = customer.phone, style = RojanTypography.Caption, color = ManagerColors.TextSecondary)
            }

            TagChip(text = customer.tag.displayLabel)
        }
    }
}

@Composable
internal fun TagChip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(ManagerColors.Gold.copy(alpha = 0.18f), RojanShapes.Circle)
            .padding(horizontal = RojanDimens.SpaceSM, vertical = RojanDimens.SpaceXS),
    ) {
        Text(text = text, style = RojanTypography.Caption, color = ManagerColors.GoldLight)
    }
}

@Composable
private fun EmptyCustomersNotice() {
    ManagerGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RojanShapes.Small,
    ) {
        Text(
            text = "مشتری‌ای با این مشخصات یافت نشد.",
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
private fun ManagerCustomersListScreenPreview() {
    RojanTheme {
        ManagerCustomersListScreen()
    }
}
