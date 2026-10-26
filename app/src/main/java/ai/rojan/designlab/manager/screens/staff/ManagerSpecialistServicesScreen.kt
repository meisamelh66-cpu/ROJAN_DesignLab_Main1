package ai.rojan.designlab.manager.screens.staff

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.domain.service.Service
import ai.rojan.designlab.manager.presentation.staff.ManagerSpecialistServicesUiState
import ai.rojan.designlab.manager.presentation.staff.ManagerSpecialistServicesViewModel
import ai.rojan.designlab.manager.presentation.staff.ManagerSpecialistServicesViewModelFactory
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTheme
import ai.rojan.designlab.ui.theme.RojanTypography
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Manager App workspace — P1-5, Manager Specialist <-> Service Assignment:
 * lets an owner/manager restrict [specialistId] to a specific subset of the
 * salon's real services, via the real `PUT`/`DELETE .../services/{serviceId}`
 * endpoints. Reached from [ai.rojan.designlab.manager.screens.staff.ManagerStaffEditScreen]'s
 * new "مدیریت خدمات" entry point, only once the specialist already exists
 * (no create-time sentinel here — same "always an existing record" shape
 * as [ai.rojan.designlab.manager.screens.customers.ManagerCustomerProfileScreen]'s
 * edit entry, not [ManagerStaffEditScreen]'s own create/edit dual-mode).
 *
 * The Customer-facing read side of this exact relationship
 * ([ai.rojan.designlab.presentation.specialist.SpecialistProfileViewModel])
 * already existed and needs zero changes — both sides read/write the same
 * real backend rows, confirmed this sprint's read-only audit.
 *
 * See [ManagerSpecialistServicesContent] for the Loading/Error/Ready state
 * dispatch, mirroring [ai.rojan.designlab.manager.screens.dashboard.ManagerDashboardScreen]/
 * [ai.rojan.designlab.manager.screens.services.ManagerServicesScreen]'s
 * (P0-1/P0-2) already-reviewed pattern.
 */
@Composable
fun ManagerSpecialistServicesScreen(
    modifier: Modifier = Modifier,
    specialistId: String,
    onBackClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val viewModel: ManagerSpecialistServicesViewModel = viewModel(
        factory = ManagerSpecialistServicesViewModelFactory(specialistId, context.applicationContext),
    )

    ManagerSpecialistServicesContent(
        uiState = viewModel.state,
        onRetry = viewModel::retry,
        onToggleService = viewModel::toggleService,
        modifier = modifier,
        onBackClick = onBackClick,
    )
}

/**
 * Stateless content split out from [ManagerSpecialistServicesScreen] so the
 * three [ManagerSpecialistServicesUiState] branches can be previewed
 * directly with a hardcoded state — no ViewModel/[LocalContext]/network
 * needed (see the three `@Preview`s below).
 */
@Composable
private fun ManagerSpecialistServicesContent(
    uiState: ManagerSpecialistServicesUiState,
    onRetry: () -> Unit,
    onToggleService: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
) {
    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item {
                RtlSectionHeader(
                    text = "خدمات قابل انجام",
                    style = RojanTypography.ScreenTitle,
                    color = ManagerColors.TextPrimary,
                    horizontalPadding = 0.dp,
                )
            }

            when (uiState) {
                is ManagerSpecialistServicesUiState.Loading -> item { ManagerSpecialistServicesLoading() }
                is ManagerSpecialistServicesUiState.Error ->
                    item { ManagerSpecialistServicesError(message = uiState.message, onRetry = onRetry) }
                is ManagerSpecialistServicesUiState.Ready -> {
                    if (uiState.isAssignedToEveryService) {
                        item { AssignedToEveryServiceBanner() }
                    }
                    if (uiState.mutationError != null) {
                        item {
                            Text(
                                text = uiState.mutationError,
                                style = RojanTypography.Caption,
                                color = RojanErrorText,
                            )
                        }
                    }
                    if (uiState.services.isEmpty()) {
                        item { EmptySpecialistServicesNotice() }
                    } else {
                        items(uiState.services, key = { it.id }) { service ->
                            ServiceAssignmentRow(
                                service = service,
                                isAssigned = uiState.isServiceAssigned(service.id),
                                isPending = service.id in uiState.pendingServiceIds,
                                onToggle = { onToggleService(service.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagerSpecialistServicesLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(RojanDimens.SpaceLG), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ManagerColors.Turquoise)
    }
}

/** Mirrors [ai.rojan.designlab.manager.screens.dashboard.ManagerDashboardScreen]'s (P0-1) error card — same components, same shape. */
@Composable
private fun ManagerSpecialistServicesError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceLG),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "بارگذاری خدمات با خطا مواجه شد",
                    style = RojanTypography.CardTitle,
                    color = ManagerColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = message,
                    style = RojanTypography.Body,
                    color = ManagerColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = RojanDimens.SpaceSM),
                )
            }
        }

        ManagerPrimaryButton(
            text = "تلاش مجدد",
            onClick = onRetry,
            modifier = Modifier.padding(top = RojanDimens.SpaceLG),
        )
    }
}

/** The backend's own real business rule, surfaced rather than left implicit: an empty assignment set means eligible for every service, not "assigned to nothing" - see [ManagerSpecialistServicesUiState.Ready.isAssignedToEveryService]'s own doc comment. */
@Composable
private fun AssignedToEveryServiceBanner() {
    ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
        Text(
            text = "این متخصص در حال حاضر واجد شرایط انجام همه‌ی خدمات سالن است. با تغییر هر مورد، فقط خدمات انتخاب‌شده باقی می‌مانند.",
            style = RojanTypography.Caption,
            color = ManagerColors.TextSecondary,
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
        )
    }
}

@Composable
private fun ServiceAssignmentRow(
    service: Service,
    isAssigned: Boolean,
    isPending: Boolean,
    onToggle: () -> Unit,
) {
    ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = service.name, style = RojanTypography.Body, color = ManagerColors.TextPrimary)
                Text(text = service.category, style = RojanTypography.Caption, color = ManagerColors.TextSecondary)
            }

            if (isPending) {
                CircularProgressIndicator(
                    color = ManagerColors.Turquoise,
                    modifier = Modifier.size(RojanDimens.SpaceLG),
                    strokeWidth = 2.dp,
                )
            } else {
                Switch(
                    checked = isAssigned,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = ManagerColors.Turquoise,
                        checkedThumbColor = ManagerColors.TurquoiseLight,
                    ),
                )
            }
        }
    }
}

@Composable
private fun EmptySpecialistServicesNotice() {
    ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.Small) {
        Text(
            text = "خدمتی برای این سالن ثبت نشده است.",
            style = RojanTypography.Body,
            color = ManagerColors.TextSecondary,
            modifier = Modifier.fillMaxWidth().padding(RojanDimens.SpaceLG),
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ManagerSpecialistServicesScreenLoadingPreview() {
    RojanTheme {
        ManagerSpecialistServicesContent(uiState = ManagerSpecialistServicesUiState.Loading, onRetry = {}, onToggleService = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ManagerSpecialistServicesScreenErrorPreview() {
    RojanTheme {
        ManagerSpecialistServicesContent(
            uiState = ManagerSpecialistServicesUiState.Error("اتصال به اینترنت برقرار نیست"),
            onRetry = {},
            onToggleService = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ManagerSpecialistServicesScreenReadyPreview() {
    val sampleServices = listOf(
        Service(id = "s1", name = "کوتاهی مو", category = "مو", price = 250000, durationMinutes = 30, active = true),
        Service(id = "s2", name = "رنگ مو", category = "مو", price = 800000, durationMinutes = 90, active = true),
        Service(id = "s3", name = "مانیکور", category = "ناخن", price = 180000, durationMinutes = 45, active = true),
    )
    RojanTheme {
        ManagerSpecialistServicesContent(
            uiState = ManagerSpecialistServicesUiState.Ready(
                services = sampleServices,
                assignedServiceIds = setOf("s1"),
                isAssignedToEveryService = false,
                pendingServiceIds = setOf("s2"),
            ),
            onRetry = {},
            onToggleService = {},
        )
    }
}
