package ai.rojan.designlab.manager.screens.customers

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerErrorState
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerGlassTheme
import ai.rojan.designlab.manager.components.ManagerLoadingState
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.domain.customer.CustomerTag
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import ai.rojan.designlab.manager.domain.customer.displayLabel
import ai.rojan.designlab.manager.presentation.customers.ManagerCustomersViewModel
import ai.rojan.designlab.manager.presentation.customers.ManagerCustomersViewModelFactory
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.ui.components.cards.PremiumCardShell
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
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
 * only the initial value, not a locked/controlled one. Tag filtering
 * remains a pure client-side filter over whatever [viewModel]'s current
 * [ManagerCustomersViewModel.state] already holds — it was never part of
 * the search query itself, and still isn't.
 *
 * Phase D (Customers ViewModel migration — UI wiring): this screen now
 * observes [ManagerCustomersViewModel] instead of reading
 * [ManagerRepositories.customers] directly. [query] changes call
 * [ManagerCustomersViewModel.searchCustomers] (which owns its own
 * debounce/cancellation/phone-normalization — unchanged from before,
 * just no longer duplicated here); [viewModel]'s `init` already issues
 * the initial unfiltered load, so no extra effect is needed for that.
 * Loading/Error states are new here (this screen never rendered them
 * before wiring) — a direct, required consequence of observing the
 * ViewModel's real state contract, not a redesign.
 *
 * Phase F1-C Refresh Fix (Phase E audit finding): returning here after
 * editing a customer (List → Profile → Edit → Save → back → back) used to
 * show the pre-edit name/phone/tag, since [viewModel]'s cached `state` is a
 * snapshot from the last search, not a live view of the underlying cache
 * [ai.rojan.designlab.manager.data.BackendCustomerRepository.update] already
 * mutated correctly. The `LifecycleResumeEffect` below re-uses the existing
 * [ManagerCustomersViewModel.retry] (a cheap, synchronous local-cache
 * re-read, not a network call) on every resume after the first one — same
 * pattern, same [hasEnteredBefore] first-entry guard, as
 * [ai.rojan.designlab.manager.screens.customers.ManagerCustomerProfileScreen]'s
 * matching fix.
 */
@Composable
fun ManagerCustomersListScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onCustomerClick: (String) -> Unit = {},
    initialTagFilter: CustomerTag? = null,
    viewModel: ManagerCustomersViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = ManagerCustomersViewModelFactory(
            // Phase F2 Timing Fix: a lambda, not the value itself - ManagerRepositories.customers is
            // read fresh every time the ViewModel invokes this, never evaluated once here.
            customerRepositoryProvider = { ManagerRepositories.customers },
            currentUserIdentityContextRepository = BackendApiContainerHolder.get(LocalContext.current).currentUserIdentityContextRepository,
        ),
    ),
) {
    var query by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf(initialTagFilter) }
    var hasEnteredBefore by rememberSaveable { mutableStateOf(false) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        if (hasEnteredBefore) {
            viewModel.retry()
        }
        hasEnteredBefore = true
        onPauseOrDispose { }
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
                    onQueryChange = { newQuery ->
                        query = newQuery
                        viewModel.searchCustomers(newQuery)
                    },
                )
            }

            item {
                CustomerTagFilterRow(
                    selectedTag = selectedTag,
                    onTagSelected = { selectedTag = it },
                )
            }

            when (val listState = viewModel.state) {
                is UiState.Loading -> item {
                    ManagerLoadingState(message = "در حال بارگذاری مشتریان...")
                }

                is UiState.Error -> item {
                    ManagerErrorState(
                        description = listState.message,
                        actionLabel = "تلاش مجدد",
                        onAction = { viewModel.retry() },
                    )
                }

                is UiState.Empty -> item { EmptyCustomersNotice() }

                is UiState.Success -> {
                    val filteredCustomers = listState.data.filter { customer ->
                        selectedTag == null || customer.tag == selectedTag
                    }
                    if (filteredCustomers.isEmpty()) {
                        item { EmptyCustomersNotice() }
                    } else {
                        items(filteredCustomers, key = { it.id }) { customer ->
                            CustomerCard(
                                customer = customer,
                                onClick = { onCustomerClick(customer.id) },
                            )
                        }
                    }
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
                                modifier = Modifier.fillMaxWidth(),
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

/**
 * Design-system refinement, Phase 4B-2: rendering moved onto the shared
 * [PremiumCardShell] (shell only — content/spacing/behavior unchanged).
 * [PremiumCardShell]'s default `variant = RojanCardVariant.GlassCard`
 * resolves to the exact same fill/border/elevation the previous direct
 * [ManagerGlassSurface] call defaulted to, and its default
 * `contentPadding` is [RojanDimens.SpaceMD] — the same value this `Row`
 * applied manually before.
 */
@Composable
private fun CustomerCard(customer: ManagerCustomer, onClick: () -> Unit) {
    PremiumCardShell(
        shape = RojanShapes.Small,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
