package ai.rojan.designlab.manager.screens.customers

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.data.ManagerRepositories
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
 */
@Composable
fun ManagerCustomersListScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onCustomerClick: (String) -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    val filteredCustomers = remember(query) {
        ManagerRepositories.customers.search(query)
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
