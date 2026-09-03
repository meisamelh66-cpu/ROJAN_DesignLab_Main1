package ai.rojan.designlab.manager.screens.services

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.data.formatDurationMinutes
import ai.rojan.designlab.manager.data.formatTomanPrice
import ai.rojan.designlab.manager.domain.service.Service
import ai.rojan.designlab.manager.presentation.services.ManagerServicesUiState
import ai.rojan.designlab.manager.presentation.services.ManagerServicesViewModel
import ai.rojan.designlab.manager.presentation.services.ManagerServicesViewModelFactory
import ai.rojan.designlab.manager.screens.customers.TagChip
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Manager App workspace — Services: list + search + create/edit/deactivate
 * (Manager Operational Foundation, Phase 6 Step 3 — create/edit/deactivate
 * complete the CRUD this screen's Step 1 MVP deliberately deferred, via
 * [onAddClick]/[onServiceClick] routing to
 * [ai.rojan.designlab.manager.screens.services.ManagerServiceEditScreen]).
 *
 * Reliability Sprint 01, P0-2: list loading now goes through
 * [ManagerServicesViewModel] instead of reading [ManagerRepositories.services]
 * unconditionally — a real sync failure now renders a retryable error
 * instead of the same "no services" empty notice a genuinely-empty
 * catalog would show. [AddServiceRow] stays reachable regardless of
 * [ManagerServicesUiState] (a deliberate product decision — creating a
 * service isn't blocked by the *list* failing to load); the search field
 * and list itself only render once [ManagerServicesUiState.Ready]. See
 * [ManagerServicesContent] for the state dispatch.
 */
@Composable
fun ManagerServicesScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onAddClick: () -> Unit = {},
    onServiceClick: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel: ManagerServicesViewModel = viewModel(
        factory = ManagerServicesViewModelFactory(context.applicationContext),
    )

    ManagerServicesContent(
        uiState = viewModel.state,
        onRetry = viewModel::retry,
        modifier = modifier,
        onBackClick = onBackClick,
        onAddClick = onAddClick,
        onServiceClick = onServiceClick,
    )
}

/**
 * Stateless content split out from [ManagerServicesScreen] so the three
 * [ManagerServicesUiState] branches can be previewed directly with a
 * hardcoded state — no [ManagerServicesViewModel]/[LocalContext]/network
 * needed (see the three `@Preview`s below). [query] is declared here,
 * above the state dispatch, so it survives Loading/Error/Ready
 * transitions rather than being recreated with each one.
 */
@Composable
private fun ManagerServicesContent(
    uiState: ManagerServicesUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onAddClick: () -> Unit = {},
    onServiceClick: (String) -> Unit = {},
) {
    var query by remember { mutableStateOf("") }

    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            item {
                RtlSectionHeader(
                    text = "خدمات",
                    style = RojanTypography.ScreenTitle,
                    color = ManagerColors.TextPrimary,
                    horizontalPadding = 0.dp,
                )
            }

            item { AddServiceRow(onClick = onAddClick) }

            when (uiState) {
                is ManagerServicesUiState.Loading -> item { ManagerServicesLoading() }
                is ManagerServicesUiState.Error -> item { ManagerServicesError(message = uiState.message, onRetry = onRetry) }
                is ManagerServicesUiState.Ready -> {
                    item {
                        ServiceSearchField(
                            query = query,
                            onQueryChange = { query = it },
                        )
                    }

                    // Deliberately not `remember`-cached: this branch is re-entered fresh
                    // every time uiState becomes Ready (cold load or post-retry), and a
                    // `remember(query)` here previously kept a stale pre-load/pre-retry list
                    // alive whenever `query` itself hadn't changed - a real bug, not a
                    // hypothetical one. Plain list filtering over this list's real size is
                    // cheap enough that no memoization is needed.
                    val allServices = ManagerRepositories.services.getAll()
                    val filteredServices = if (query.isBlank()) {
                        allServices
                    } else {
                        allServices.filter { it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true) }
                    }

                    if (filteredServices.isEmpty()) {
                        item { EmptyServicesNotice() }
                    } else {
                        items(filteredServices) { service ->
                            ServiceCard(service = service, onClick = { onServiceClick(service.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagerServicesLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(RojanDimens.SpaceLG), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ManagerColors.Turquoise)
    }
}

/** Mirrors [ai.rojan.designlab.manager.screens.dashboard.ManagerDashboardScreen]'s (P0-1) error card — same components, same shape, same proven Manager-palette-correct pattern. */
@Composable
private fun ManagerServicesError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RojanDimens.SpaceLG),
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

@Composable
private fun AddServiceRow(onClick: () -> Unit) {
    ManagerGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .rojanPressable(onClick = onClick),
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
                imageVector = Icons.Filled.AddCircle,
                contentDescription = "افزودن خدمت",
                size = RojanIconSize.Medium,
                tint = ManagerColors.Gold,
            )
            Text(text = "افزودن خدمت", style = RojanTypography.Body, color = ManagerColors.TextPrimary)
        }
    }
}

@Composable
private fun ServiceSearchField(query: String, onQueryChange: (String) -> Unit) {
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
                                text = "جستجوی نام یا دسته‌بندی...",
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
private fun ServiceCard(service: Service, onClick: () -> Unit) {
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
            RojanIconContainer(
                imageVector = Icons.Filled.ContentCut,
                contentDescription = null,
                size = RojanIconSize.Medium,
                tint = ManagerColors.Turquoise,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(text = service.name, style = RojanTypography.Body, color = ManagerColors.TextPrimary)
                Text(
                    text = "${service.category} · ${formatDurationMinutes(service.durationMinutes)} · ${formatTomanPrice(service.price)}",
                    style = RojanTypography.Caption,
                    color = ManagerColors.TextSecondary,
                )
            }

            TagChip(text = if (service.active) "فعال" else "غیرفعال")
        }
    }
}

@Composable
private fun EmptyServicesNotice() {
    ManagerGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RojanShapes.Small,
    ) {
        Text(
            text = "خدمتی برای این سالن ثبت نشده است.",
            style = RojanTypography.Body,
            color = ManagerColors.TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceLG),
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ManagerServicesScreenLoadingPreview() {
    RojanTheme {
        ManagerServicesContent(uiState = ManagerServicesUiState.Loading, onRetry = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ManagerServicesScreenErrorPreview() {
    RojanTheme {
        ManagerServicesContent(
            uiState = ManagerServicesUiState.Error("اتصال به اینترنت برقرار نیست"),
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ManagerServicesScreenReadyPreview() {
    RojanTheme {
        ManagerServicesContent(uiState = ManagerServicesUiState.Ready, onRetry = {})
    }
}
