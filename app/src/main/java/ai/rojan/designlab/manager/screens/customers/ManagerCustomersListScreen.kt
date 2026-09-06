package ai.rojan.designlab.manager.screens.customers

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.repository.SalonCustomer
import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerEmptyState
import ai.rojan.designlab.manager.components.ManagerErrorState
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerLoadingState
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.presentation.customers.ManagerCustomersViewModel
import ai.rojan.designlab.manager.presentation.customers.ManagerCustomersViewModelFactory
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.text.withDirectionFor
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Manager App workspace — Customers: list + search.
 *
 * **FIX-004 (PARTIAL — Manager Customers Real Data):** the roster is now
 * the salon's real backend customers (`GET /api/v1/salons/{salonId}/customers`
 * via [ManagerCustomersViewModel], the same real source the booking
 * wizard's customer step uses), not the in-memory
 * [ai.rojan.designlab.manager.data.InMemoryCustomerRepository] sample
 * list. A real [SalonCustomer] carries only name + email, so the card no
 * longer shows a phone number, "last visit", or a customer tag — those
 * had no backend source. Loading / empty / error use the standard
 * `Manager*State` views; a backend failure never falls back to fake data.
 *
 * Out of FIX-004 scope, unchanged: [ManagerCustomerProfileScreen] (still
 * in-memory), and note that tapping a row now passes a real backend
 * customer id that the still-in-memory profile screen cannot resolve — it
 * falls back to its first sample customer until the profile screen is
 * migrated in a later task.
 *
 * ROJAN AI Manager Visual Theme: dark luxury background
 * ([ManagerScaffold]/[ManagerGlassSurface]) — visual language unchanged.
 */
@Composable
fun ManagerCustomersListScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onCustomerClick: (String) -> Unit = {},
    viewModel: ManagerCustomersViewModel = viewModel(
        factory = ManagerCustomersViewModelFactory(
            salonRepository = BackendApiContainerHolder.get(LocalContext.current).salonRepository,
            salonCustomerRepository = BackendApiContainerHolder.get(LocalContext.current).salonCustomerRepository,
        ),
    ),
) {
    var query by remember { mutableStateOf("") }

    LaunchedEffect(query) {
        viewModel.search(query)
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

            when (val listState = viewModel.state) {
                is UiState.Loading -> item {
                    ManagerLoadingState(message = "در حال بارگذاری مشتریان...")
                }

                is UiState.Error -> item {
                    ManagerErrorState(
                        description = listState.message,
                        actionLabel = "تلاش مجدد",
                        onAction = { viewModel.search(query) },
                    )
                }

                is UiState.Empty -> item {
                    ManagerEmptyState(
                        title = "مشتری‌ای یافت نشد",
                        description = "فقط مشتریانی که قبلاً در این سالن نوبت داشته‌اند اینجا نمایش داده می‌شوند.",
                    )
                }

                is UiState.Success -> items(listState.data) { customer ->
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
                                text = "جستجوی نام یا ایمیل...",
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
private fun CustomerCard(customer: SalonCustomer, onClick: () -> Unit) {
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
                    text = customer.fullName.take(1),
                    style = RojanTypography.CardTitle,
                    color = ManagerColors.TurquoiseLight,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = customer.fullName, style = RojanTypography.Body, color = ManagerColors.TextPrimary)
                Text(text = customer.email, style = RojanTypography.Caption, color = ManagerColors.TextSecondary)
            }
        }
    }
}

/**
 * Kept for [ManagerCustomerProfileScreen], which still renders the
 * in-memory customer tag (out of FIX-004 scope). Not used by this screen
 * any more — a real [SalonCustomer] has no tag.
 */
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
