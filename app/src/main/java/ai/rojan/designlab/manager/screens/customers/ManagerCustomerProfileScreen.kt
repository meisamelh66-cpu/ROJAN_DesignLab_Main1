package ai.rojan.designlab.manager.screens.customers

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerIconContainer
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.domain.customer.CustomerServiceHistoryEntry
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import ai.rojan.designlab.manager.domain.customer.displayLabel
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.text.Text
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Manager App workspace — Customer Profile foundation: identity header,
 * service history, and manager notes. Additive-only, same primitives as
 * the rest of the Manager MVP. Data sourced from
 * [ManagerRepositories.customers] (Manager Domain Foundation Phase 1) —
 * no backend.
 *
 * ROJAN AI Manager Visual Theme Implementation: re-themed for the dark
 * luxury background — content/data/navigation unchanged.
 *
 * "Foundation" per this pass's scope: [onEditNotesClick] is present but
 * inert (`{}` default) — no note-editing UI/persistence yet.
 */
@Composable
fun ManagerCustomerProfileScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    customerId: String = "c1",
    onEditNotesClick: () -> Unit = {},
) {
    val customer = ManagerRepositories.customers.getById(customerId)
        ?: ManagerRepositories.customers.getAll().first()
    val history = ManagerRepositories.customers.getServiceHistory(customerId)
    val notes = customer.notes

    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceLG),
        ) {
            item { CustomerIdentityHeader(customer) }
            item { ServiceHistorySection(history) }
            item { ManagerNotesSection(notes = notes, onEditClick = onEditNotesClick) }
        }
    }
}

@Composable
private fun CustomerIdentityHeader(customer: ManagerCustomer) {
    ManagerGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RojanShapes.GlassCard,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(ManagerColors.Turquoise.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = customer.name.take(1),
                    style = RojanTypography.ScreenTitle,
                    color = ManagerColors.TurquoiseLight,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = customer.name, style = RojanTypography.CardTitle, color = ManagerColors.TextPrimary)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                    modifier = Modifier.padding(top = RojanDimens.SpaceXS),
                ) {
                    RojanIconContainer(
                        imageVector = Icons.Filled.Phone,
                        contentDescription = null,
                        size = RojanIconSize.Small,
                        tint = ManagerColors.TextSecondary,
                    )
                    Text(text = customer.phone, style = RojanTypography.Caption, color = ManagerColors.TextSecondary)
                }
                Text(
                    text = "${customer.totalVisits} نوبت گذشته",
                    style = RojanTypography.Caption,
                    color = ManagerColors.TextSecondary,
                    modifier = Modifier.padding(top = RojanDimens.SpaceXS),
                )
                TagChip(text = customer.tag.displayLabel, modifier = Modifier.padding(top = RojanDimens.SpaceSM))
            }
        }
    }
}

@Composable
private fun ServiceHistorySection(history: List<CustomerServiceHistoryEntry>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        RtlSectionHeader(
            text = "سابقه خدمات",
            style = RojanTypography.SectionTitle,
            color = ManagerColors.TextPrimary,
            horizontalPadding = 0.dp,
        )

        ManagerGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = RojanDimens.SpaceMD),
            shape = RojanShapes.GlassCard,
        ) {
            Column(modifier = Modifier.padding(RojanDimens.SpaceMD)) {
                history.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = entry.service, style = RojanTypography.Body, color = ManagerColors.TextPrimary)
                            Text(
                                text = "${entry.specialist} · ${entry.date}",
                                style = RojanTypography.Caption,
                                color = ManagerColors.TextSecondary,
                            )
                        }
                        Text(text = entry.price, style = RojanTypography.Body, color = ManagerColors.GoldLight)
                    }
                    if (index != history.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = RojanDimens.SpaceSM)
                                .height(1.dp)
                                .background(ManagerColors.TextSecondary.copy(alpha = 0.16f)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagerNotesSection(notes: String?, onEditClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        RtlSectionHeader(
            text = "یادداشت مدیر",
            style = RojanTypography.SectionTitle,
            color = ManagerColors.TextPrimary,
            horizontalPadding = 0.dp,
        )

        ManagerGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .rojanPressable(onClick = onEditClick)
                .padding(top = RojanDimens.SpaceMD),
            shape = RojanShapes.GlassCard,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RojanDimens.SpaceMD),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
            ) {
                ManagerIconContainer(
                    imageVector = Icons.Filled.EditNote,
                    contentDescription = "ویرایش یادداشت",
                    containerSize = 44.dp,
                    accentColor = ManagerColors.Gold,
                )
                Text(
                    text = notes ?: "هنوز یادداشتی برای این مشتری ثبت نشده است.",
                    style = RojanTypography.Body,
                    color = if (notes != null) ManagerColors.TextPrimary else ManagerColors.TextSecondary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
)
@Composable
private fun ManagerCustomerProfileScreenPreview() {
    RojanTheme {
        ManagerCustomerProfileScreen(customerId = "c1")
    }
}
