package ai.rojan.designlab.manager.screens.booking

import ai.rojan.designlab.domain.repository.SalonCustomer
import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerEmptyState
import ai.rojan.designlab.manager.components.ManagerErrorState
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerLoadingState
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModel
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
import androidx.compose.ui.unit.dp

/**
 * Manager Booking Journey — step 1: pick which customer this appointment
 * is for.
 *
 * **Manager Booking Creation Integrity follow-up:** searches the salon's
 * real customer roster (`GET /salons/{salonId}/customers`, via
 * [ManagerBookingViewModel.searchCustomers]) — only accounts who have
 * actually booked with this salon before, never a global user directory
 * and never a fabricated identity. Replaces the previous
 * `ManagerRepositories.customers` in-memory sample list. A real
 * [SalonCustomer] carries a name and email, not the old demo model's
 * name/phone/tag — there is no phone number or customer-tag concept on a
 * real backend account.
 */
@Composable
fun ManagerBookingCustomerScreen(
    viewModel: ManagerBookingViewModel,
    onBackClick: (() -> Unit)? = null,
    onCustomerSelected: () -> Unit = {},
) {
    var query by remember { mutableStateOf("") }

    LaunchedEffect(query) {
        viewModel.searchCustomers(query)
    }

    ManagerScaffold(onBackClick = onBackClick) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item {
                RtlSectionHeader(
                    text = "انتخاب مشتری",
                    style = RojanTypography.ScreenTitle,
                    color = ManagerColors.TextPrimary,
                    horizontalPadding = 0.dp,
                )
            }

            item {
                BookingSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "جستجوی نام یا ایمیل...",
                )
            }

            when (val searchState = viewModel.customerSearchState) {
                is UiState.Loading -> item { ManagerLoadingState(message = "در حال جستجو...") }
                is UiState.Error -> item {
                    ManagerErrorState(
                        description = searchState.message,
                        actionLabel = "تلاش مجدد",
                        onAction = { viewModel.searchCustomers(query) },
                    )
                }
                is UiState.Empty -> item {
                    ManagerEmptyState(
                        title = "مشتری‌ای یافت نشد",
                        description = "فقط مشتریانی که قبلاً در این سالن نوبت داشته‌اند اینجا نمایش داده می‌شوند.",
                    )
                }
                is UiState.Success -> items(searchState.data) { customer ->
                    BookingCustomerRow(
                        customer = customer,
                        onClick = {
                            viewModel.selectCustomer(customer.id)
                            onCustomerSelected()
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun BookingSearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String) {
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
                            Text(text = placeholder, style = RojanTypography.Body, color = ManagerColors.TextSecondary)
                        }
                        innerTextField()
                    },
                )
            }
        }
    }
}

@Composable
private fun BookingCustomerRow(customer: SalonCustomer, onClick: () -> Unit) {
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
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = customer.fullName, style = RojanTypography.Body, color = ManagerColors.TextPrimary)
                Text(text = customer.email, style = RojanTypography.Caption, color = ManagerColors.TextSecondary)
            }
        }
    }
}
