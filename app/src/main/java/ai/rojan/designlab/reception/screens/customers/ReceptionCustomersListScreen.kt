package ai.rojan.designlab.reception.screens.customers

import ai.rojan.designlab.reception.components.ReceptionCustomerIdentityCard
import ai.rojan.designlab.reception.components.ReceptionScaffold
import ai.rojan.designlab.reception.components.ReceptionUiStateList
import ai.rojan.designlab.reception.presentation.customers.ReceptionCustomersViewModel
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.ReceptionPalette
import ai.rojan.designlab.ui.theme.RojanDimens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.rojan.designlab.ui.theme.RojanTypography

/**
 * `VIEW_CRM`-scoped customer search — real, read-only. No note/tag
 * mutation, no create/edit — Reception's fixed permission tier (System 1
 * decision §1c) has no `MANAGE_CRM`, so this screen has no affordance for
 * it either.
 */
@Composable
fun ReceptionCustomersListScreen(
    viewModel: ReceptionCustomersViewModel,
    onBackClick: () -> Unit,
) {
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    ReceptionScaffold(onBackClick = onBackClick) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD)) {
            Text(text = "مشتریان", style = RojanTypography.ScreenTitle, color = ReceptionPalette.textPrimary)

            OutlinedTextField(
                value = query,
                onValueChange = { query = it; viewModel.search(it.ifBlank { null }) },
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
                ReceptionCustomerIdentityCard(customer = customer)
            }
        }
    }
}
