package ai.rojan.designlab.reception.components

import ai.rojan.designlab.reception.domain.repository.ReceptionCustomer
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.ReceptionPalette
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Reusable customer-identity display — Reception's `VIEW_CRM`-scoped
 * counterpart to Manager's own customer-list card, deliberately showing
 * only [ReceptionCustomer.fullName]/[ReceptionCustomer.phoneNumber] (no
 * lifetime value, no tags, no note affordance — none of that is in scope
 * for a read-only membership permission). Reused by
 * [ai.rojan.designlab.reception.screens.customers.ReceptionCustomersListScreen]
 * and available to the booking wizard's customer-select step or any
 * future screen that needs to display "who this booking/record is for"
 * without re-deriving the layout.
 */
@Composable
fun ReceptionCustomerIdentityCard(
    customer: ReceptionCustomer,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    ReceptionGlassSurface(
        modifier = modifier.fillMaxWidth().let { if (onClick != null) it.rojanPressable(onClick = onClick) else it },
        shape = RojanShapes.Small,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = customer.fullName, style = RojanTypography.CardTitle, color = ReceptionPalette.textPrimary)
                if (customer.phoneNumber != null) {
                    Text(
                        text = customer.phoneNumber,
                        style = RojanTypography.Caption,
                        color = ReceptionPalette.textSecondary,
                        modifier = Modifier.padding(top = RojanDimens.SpaceXS),
                    )
                }
            }
            if (!customer.active) {
                Text(text = "غیرفعال", style = RojanTypography.Caption, color = ReceptionPalette.textSecondary)
            }
        }
    }
}
