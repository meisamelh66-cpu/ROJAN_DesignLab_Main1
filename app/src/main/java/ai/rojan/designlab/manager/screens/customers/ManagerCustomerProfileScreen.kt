package ai.rojan.designlab.manager.screens.customers

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerEmptyState
import ai.rojan.designlab.manager.components.ManagerErrorState
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerIconContainer
import ai.rojan.designlab.manager.components.ManagerLoadingState
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.domain.customer.CustomerServiceHistoryEntry
import ai.rojan.designlab.manager.domain.customer.ManagerCustomerProfile
import ai.rojan.designlab.manager.presentation.customers.ManagerCustomerProfileViewModel
import ai.rojan.designlab.manager.presentation.customers.ManagerCustomerProfileViewModelFactory
import ai.rojan.designlab.presentation.common.UiState
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Manager App workspace — Customer Profile: identity header, service
 * history, and manager notes.
 *
 * **FIX-006 (Manager Customer Profile Real Data):** the record is now the
 * salon's real backend CRM customer (`GET /customer-records/{id}` +
 * `/notes` + `/tags` + `/timeline` + `/bookings` via
 * [ManagerCustomerProfileViewModel]), not the in-memory
 * [ai.rojan.designlab.manager.data.InMemoryCustomerRepository] sample
 * record. `customerId` is the id the Customers list navigates with (a
 * linked account's `UserId`); the repository resolves it to the salon's
 * CRM `CustomerId`. Loading / empty / error use the standard
 * `Manager*State` views; a backend failure never falls back to fake data.
 *
 * ROJAN AI Manager Visual Theme: dark luxury background — the section
 * layout is unchanged from the previous pass.
 *
 * "Foundation" per this screen's scope: [onEditNotesClick] is present but
 * inert (`{}` default) — no note-editing UI/persistence yet.
 */
@Composable
fun ManagerCustomerProfileScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    customerId: String = "",
    onEditNotesClick: () -> Unit = {},
    viewModel: ManagerCustomerProfileViewModel = viewModel(
        factory = ManagerCustomerProfileViewModelFactory(
            accountId = customerId,
            salonRepository = BackendApiContainerHolder.get(LocalContext.current).salonRepository,
            profileRepository = BackendApiContainerHolder.get(LocalContext.current).managerCustomerProfileRepository,
        ),
    ),
) {
    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        when (val state = viewModel.state) {
            is UiState.Loading -> ManagerLoadingState(message = "در حال بارگذاری پروفایل مشتری...")

            is UiState.Error -> ManagerErrorState(
                description = state.message,
                actionLabel = "تلاش مجدد",
                onAction = { viewModel.load() },
            )

            is UiState.Empty -> ManagerEmptyState(
                title = "پروفایلی یافت نشد",
                description = "برای این مشتری هنوز سابقه‌ای در این سالن ثبت نشده است.",
            )

            is UiState.Success -> ManagerCustomerProfileContent(
                profile = state.data,
                onEditNotesClick = onEditNotesClick,
            )
        }
    }
}

/** Stateless body — the screen's rendered form once the profile has loaded. Seam for previews and screenshot tests. */
@Composable
internal fun ManagerCustomerProfileContent(
    profile: ManagerCustomerProfile,
    onEditNotesClick: () -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceLG),
    ) {
        item { CustomerIdentityHeader(profile) }
        item { ServiceHistorySection(profile.history) }
        item { ManagerNotesSection(notes = profile.notes, onEditClick = onEditNotesClick) }
    }
}

@Composable
private fun CustomerIdentityHeader(profile: ManagerCustomerProfile) {
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
                    text = profile.fullName.take(1),
                    style = RojanTypography.ScreenTitle,
                    color = ManagerColors.TurquoiseLight,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = profile.fullName, style = RojanTypography.CardTitle, color = ManagerColors.TextPrimary)
                if (profile.phone.isNotBlank()) {
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
                        Text(text = profile.phone, style = RojanTypography.Caption, color = ManagerColors.TextSecondary)
                    }
                }
                Text(
                    text = "${profile.totalVisits} نوبت گذشته",
                    style = RojanTypography.Caption,
                    color = ManagerColors.TextSecondary,
                    modifier = Modifier.padding(top = RojanDimens.SpaceXS),
                )
                TagChip(text = profile.statusLabel, modifier = Modifier.padding(top = RojanDimens.SpaceSM))
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
                if (history.isEmpty()) {
                    Text(
                        text = "سابقه خدماتی برای این مشتری ثبت نشده است.",
                        style = RojanTypography.Body,
                        color = ManagerColors.TextSecondary,
                    )
                }
                history.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = entry.service, style = RojanTypography.Body, color = ManagerColors.TextPrimary)
                            Text(
                                text = if (entry.specialist.isBlank()) entry.date else "${entry.specialist} · ${entry.date}",
                                style = RojanTypography.Caption,
                                color = ManagerColors.TextSecondary,
                            )
                        }
                        if (entry.price.isNotBlank()) {
                            Text(text = entry.price, style = RojanTypography.Body, color = ManagerColors.GoldLight)
                        }
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
private fun ManagerCustomerProfileContentPreview() {
    RojanTheme {
        ManagerCustomerProfileContent(
            profile = ManagerCustomerProfile(
                fullName = "سارا محمدی",
                phone = "۰۹۱۲۱۲۳۴۵۶۷",
                statusLabel = "مشتری فعال · VIP",
                totalVisits = 12,
                history = listOf(
                    CustomerServiceHistoryEntry("2026-05-01", "نوبت ایجاد شد", "", ""),
                    CustomerServiceHistoryEntry("2026-04-10", "نوبت تکمیل شد", "", ""),
                ),
                notes = "ترجیح می‌دهد وقت‌های عصر رزرو کند.",
            ),
            onEditNotesClick = {},
        )
    }
}
