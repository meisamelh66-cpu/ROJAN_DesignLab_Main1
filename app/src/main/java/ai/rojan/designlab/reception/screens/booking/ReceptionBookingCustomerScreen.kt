package ai.rojan.designlab.reception.screens.booking

import ai.rojan.designlab.reception.components.ReceptionCustomerIdentityCard
import ai.rojan.designlab.reception.components.ReceptionScaffold
import ai.rojan.designlab.reception.components.ReceptionUiStateList
import ai.rojan.designlab.reception.presentation.booking.ReceptionBookingViewModel
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.ReceptionPalette
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Existing-customer search/select — per `ROJAN_System1_Backend_Decision_v2.md` §1c, Reception only ever books an already-linked existing customer (`VIEW_CRM`, read-only) — there is no walk-in/create-customer entry point on this screen. */
@Composable
fun ReceptionBookingCustomerScreen(
    viewModel: ReceptionBookingViewModel,
    onBackClick: () -> Unit,
    onCustomerSelected: () -> Unit,
) {
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.searchCustomers(null) }

    ReceptionScaffold(onBackClick = onBackClick) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD)) {
            Text(text = "انتخاب مشتری", style = RojanTypography.ScreenTitle, color = ReceptionPalette.textPrimary)

            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.searchCustomers(it.ifBlank { null })
                },
                label = { Text("جست‌وجو با نام یا شماره موبایل") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = ReceptionPalette.textPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ReceptionPalette.textAccent,
                    unfocusedBorderColor = ReceptionPalette.textSecondary,
                    cursorColor = ReceptionPalette.textAccent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            ReceptionUiStateList(
                state = customers,
                emptyMessage = "مشتری‌ای یافت نشد",
                modifier = Modifier.fillMaxSize(),
            ) { customer ->
                ReceptionCustomerIdentityCard(customer = customer, onClick = {
                    viewModel.selectCustomer(customer)
                    onCustomerSelected()
                })
            }
        }
    }
}
