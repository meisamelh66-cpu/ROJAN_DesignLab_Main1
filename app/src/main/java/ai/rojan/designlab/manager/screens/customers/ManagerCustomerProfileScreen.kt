package ai.rojan.designlab.manager.screens.customers

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerIconContainer
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.domain.customer.CustomerNote
import ai.rojan.designlab.manager.domain.customer.CustomerServiceHistoryEntry
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import ai.rojan.designlab.manager.domain.customer.displayLabel
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Manager App workspace — Customer Profile: identity header, service
 * history, and manager notes. Data sourced from
 * [ManagerRepositories.customers] (Phase 2, M2 — real backend Customer
 * CRM API). The bulk-listed [ManagerRepositories.customers]-cached
 * fields (name/phone/tag) render immediately; visit history and the
 * latest note are per-customer detail the bulk listing doesn't include
 * (see [ai.rojan.designlab.manager.data.BackendCustomerRepository]'s own
 * doc comment), so [LaunchedEffect] fetches them for just this one
 * customer on entry — a single detail view, not the N+1 case Phase 1
 * ruled out for list screens.
 *
 * ROJAN AI Manager Visual Theme Implementation: re-themed for the dark
 * luxury background — content/data/navigation unchanged.
 *
 * CRM Foundation, Phase 6 Step 5: the notes section now shows the full,
 * real note history via [ManagerRepositories.customers]'s
 * `getNoteHistory` (previously a single "tap to edit" row backed by only
 * the latest note — dropped along with the `onEditNotesClick` callback it
 * existed for, since the backend has no note-creation endpoint to edit
 * *into*; see that repository's own doc comment). Read-only, same as the
 * service history section below it.
 */
@Composable
fun ManagerCustomerProfileScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    customerId: String = "c1",
) {
    var isLoadingDetail by remember(customerId) { mutableStateOf(true) }

    LaunchedEffect(customerId) {
        isLoadingDetail = true
        ManagerRepositories.customers.loadDetail(customerId)
        isLoadingDetail = false
    }

    val customer = ManagerRepositories.customers.getById(customerId)
        ?: ManagerRepositories.customers.getAll().firstOrNull()

    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        if (customer == null) {
            return@ManagerScaffold
        }

        val history = ManagerRepositories.customers.getServiceHistory(customerId)
        val noteHistory = ManagerRepositories.customers.getNoteHistory(customerId)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceLG),
        ) {
            item { CustomerIdentityHeader(customer) }
            if (!isLoadingDetail) {
                item { ServiceHistorySection(history) }
                item { ManagerNotesSection(notes = noteHistory) }
            }
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

/** CRM Foundation, Phase 6 Step 5 — every real manager note on this customer, newest first (see [CustomerNote]'s own doc comment on why read-only). */
@Composable
private fun ManagerNotesSection(notes: List<CustomerNote>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        RtlSectionHeader(
            text = "یادداشت‌های مدیر",
            style = RojanTypography.SectionTitle,
            color = ManagerColors.TextPrimary,
            horizontalPadding = 0.dp,
        )

        if (notes.isEmpty()) {
            ManagerGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
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
                        contentDescription = null,
                        containerSize = 44.dp,
                        accentColor = ManagerColors.Gold,
                    )
                    Text(
                        text = "هنوز یادداشتی برای این مشتری ثبت نشده است.",
                        style = RojanTypography.Body,
                        color = ManagerColors.TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = RojanDimens.SpaceMD),
                verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
            ) {
                notes.forEach { note -> ManagerNoteRow(note) }
            }
        }
    }
}

@Composable
private fun ManagerNoteRow(note: CustomerNote) {
    ManagerGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RojanShapes.GlassCard,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
        ) {
            Text(text = note.text, style = RojanTypography.Body, color = ManagerColors.TextPrimary)
            Text(
                text = note.createdAt,
                style = RojanTypography.Caption,
                color = ManagerColors.TextSecondary,
                modifier = Modifier.padding(top = RojanDimens.SpaceXS),
            )
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
