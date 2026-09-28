package ai.rojan.designlab.manager.screens.customers

import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerEmptyState
import ai.rojan.designlab.manager.components.ManagerErrorState
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerIconContainer
import ai.rojan.designlab.manager.components.ManagerLoadingState
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.manager.components.ManagerScaffold
import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.domain.ai.ManagerCrmInsight
import ai.rojan.designlab.manager.domain.customer.CustomerNote
import ai.rojan.designlab.manager.domain.customer.CustomerServiceHistoryEntry
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import ai.rojan.designlab.manager.domain.customer.displayLabel
import ai.rojan.designlab.manager.presentation.customers.ManagerCustomerProfileViewModel
import ai.rojan.designlab.manager.presentation.customers.ManagerCustomerProfileViewModelFactory
import ai.rojan.designlab.manager.presentation.customers.NoteSubmissionState
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.components.rtl.RtlSectionHeader
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanErrorText
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTheme
import ai.rojan.designlab.ui.theme.RojanTypography
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
 * existed for). Same as the service history section below it, apart from
 * the create form Phase F4 adds directly to it (see [ManagerNotesSection]).
 *
 * Phase F4 (Customer Notes completion): Notes gains real Create, via
 * [ManagerCustomerProfileViewModel.submitNote] and the backend's real,
 * already-tested `POST .../notes`. The backend stays the sole source of
 * truth for note content/order — a successful submit re-runs the same full
 * [ManagerCustomerProfileViewModel.load] [retry] already triggers, rather
 * than appending locally, so this section always renders exactly what the
 * backend just returned. A failed or in-flight submission is tracked by
 * [ManagerCustomerProfileViewModel.noteSubmissionState], independent of
 * [ManagerCustomerProfileViewModel.state] — it never regresses the already-
 * loaded profile to a loading/error screen.
 *
 * Insight → Customer Profile Action, Phase 7 Step 7: also reads
 * [ManagerRepositories.crmInsights] (already populated by the same
 * [ManagerRepositories.initialize] that resolved [customer] itself - no
 * new fetch), filtered to [ManagerCrmInsight.customerId] == [customerId],
 * and renders an "پیشنهاد هوش مصنوعی" section only when at least one
 * matches - nothing added, nothing changed, for a customer with none.
 * Distinct from [CustomerIdentityHeader]'s existing [TagChip]: the chip
 * states the customer's raw status; this section states which real CRM
 * rule fired and why (see [AiInsightRow]) - reached only via the
 * unchanged, already-existing navigation into this screen, no new route.
 *
 * Customer Edit Flow, Phase 9 Step 1: [onEditClick] is
 * [CustomerIdentityHeader]'s new edit icon, routing to
 * [ai.rojan.designlab.manager.screens.customers.ManagerCustomerEditScreen] -
 * the only change to this screen for that flow; everything else here
 * (insight section, service history, notes) is unchanged.
 *
 * Customer Contact Action, Phase 9 Step 3: the phone row is now tappable
 * (when [ManagerCustomer.phone] is non-blank) and launches
 * `Intent.ACTION_DIAL` - the exact same pattern already used by
 * [ai.rojan.designlab.screens.salon.SalonDetailsScreen] for a salon's
 * phone number, not a new one. `ACTION_DIAL` only opens the dialer
 * pre-filled - it never places the call itself, so no `CALL_PHONE`
 * runtime permission is needed. The launch is wrapped in `runCatching`
 * the same way that existing call site already is, in case no dialer app
 * is available on the device; a failure is silently swallowed rather
 * than shown as an error, matching that same precedent exactly. Display
 * text/layout is otherwise unchanged.
 *
 * P0 Safety Fix (Manager Completeness Audit v1, finding P0-1): this screen
 * used to resolve [customerId] via `getById(customerId) ?: getAll().firstOrNull()`
 * — on a lookup miss it silently substituted a *different* customer, and
 * [onEditClick] would then act on the wrong customer's real id.
 *
 * Phase D (Customers ViewModel migration — UI wiring): the screen-local
 * `CustomerProfileLookupState`/`resolveCustomerProfileState` resolver that fixed
 * P0-1 has been superseded by observing [ManagerCustomerProfileViewModel]
 * directly — [ManagerCustomerProfileViewModel.load] independently upholds the
 * exact same contract (verified by its own Phase C tests): [customerId] is the
 * only identity source passed into the ViewModel's constructor and every
 * repository call it makes; a lookup miss is [UiState.Empty], never a
 * substitute customer; a real [ai.rojan.designlab.manager.domain.repository.CustomerRepository.loadDetail]
 * failure is always [UiState.Error], never silently reinterpreted as
 * not-found. `CustomerProfileLookupState`/`resolveCustomerProfileState` are kept
 * below, unused by this composable, solely because
 * `ManagerCustomerProfileScreenStateTest.kt` (committed with the original P0
 * fix, outside this phase's authorized files) still exercises them directly;
 * removing them would break that test's compilation, which this phase is
 * explicitly not authorized to modify.
 *
 * The screen still renders the same four visual states as before — Loading
 * (while [ManagerCustomerProfileViewModel] is loading), NotFound (mapped from
 * [UiState.Empty] — a real "customer not found" card with a way back to the
 * list), Error (via the same [ManagerErrorState] + "تلاش مجدد" retry
 * convention, now calling [ManagerCustomerProfileViewModel.retry]), or the
 * full profile (mapped from [UiState.Success]) — never a blank or
 * partially-substituted profile.
 *
 * Phase F1-B Refresh Fix (Phase E audit finding): editing this customer and
 * saving pops back to this exact, already-existing screen instance — its
 * [ManagerCustomerProfileViewModel] survives that round trip (correctly; it's
 * scoped to this destination's back-stack entry), so its `init`-time load is
 * not re-run automatically and the profile used to keep showing pre-edit
 * data. The `LifecycleResumeEffect` below re-uses the existing
 * [ManagerCustomerProfileViewModel.retry] — the same mechanism the Error
 * state's own retry button already calls — on every resume *after* the
 * first one, mirroring the exact `LifecycleResumeEffect` pattern already
 * established by [ai.rojan.designlab.screens.search.SearchScreen]/
 * `SalonListScreen` for an analogous stale-after-returning problem. Gated by
 * [hasEnteredBefore] (persisted via `rememberSaveable`, which survives this
 * screen being temporarily removed from composition while Edit is on top -
 * the same guarantee Navigation-Compose already gives any back-stack entry)
 * so the *first* entry is never double-loaded — `init` already covers it.
 * No ViewModel/Factory/nav-graph change was needed or made.
 */
@Composable
fun ManagerCustomerProfileScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    customerId: String = "c1",
    onEditClick: () -> Unit = {},
    viewModel: ManagerCustomerProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        key = customerId,
        factory = ManagerCustomerProfileViewModelFactory(
            // Phase F2 Timing Fix: a lambda, not the value itself - ManagerRepositories.customers is
            // read fresh every time the ViewModel invokes this, never evaluated once here.
            customerRepositoryProvider = { ManagerRepositories.customers },
            currentUserIdentityContextRepository = BackendApiContainerHolder.get(LocalContext.current).currentUserIdentityContextRepository,
            customerId = customerId,
        ),
    ),
) {
    var hasEnteredBefore by rememberSaveable(customerId) { mutableStateOf(false) }
    androidx.lifecycle.compose.LifecycleResumeEffect(customerId) {
        if (hasEnteredBefore) {
            viewModel.retry()
        }
        hasEnteredBefore = true
        onPauseOrDispose { }
    }

    ManagerScaffold(modifier = modifier, onBackClick = onBackClick) {
        when (val state = viewModel.state) {
            is UiState.Loading -> {
                ManagerLoadingState(
                    modifier = Modifier.padding(RojanDimens.SpaceMD),
                    message = "در حال بارگذاری اطلاعات مشتری...",
                )
            }

            is UiState.Empty -> {
                Column(modifier = Modifier.padding(RojanDimens.SpaceMD)) {
                    ManagerEmptyState(
                        title = "مشتری یافت نشد",
                        description = "این مشتری دیگر در دسترس نیست یا حذف شده است.",
                    )
                    if (onBackClick != null) {
                        ManagerPrimaryButton(
                            text = "بازگشت به لیست مشتریان",
                            onClick = onBackClick,
                            modifier = Modifier.padding(top = RojanDimens.SpaceMD),
                        )
                    }
                }
            }

            is UiState.Error -> {
                ManagerErrorState(
                    modifier = Modifier.padding(RojanDimens.SpaceMD),
                    description = state.message,
                    actionLabel = "تلاش مجدد",
                    onAction = { viewModel.retry() },
                )
            }

            is UiState.Success -> {
                val customer = state.data.customer
                val history = state.data.history
                val noteHistory = state.data.notes
                val customerInsights = ManagerRepositories.crmInsights.filter { it.customerId == customerId }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceLG),
                ) {
                    item { CustomerIdentityHeader(customer, onEditClick = onEditClick) }
                    if (customerInsights.isNotEmpty()) {
                        item { AiInsightSection(customerInsights) }
                    }
                    item { ServiceHistorySection(history) }
                    item {
                        ManagerNotesSection(
                            notes = noteHistory,
                            submissionState = viewModel.noteSubmissionState,
                            onSubmitNote = viewModel::submitNote,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The four honest outcomes of resolving `ManagerCustomerProfileScreen`'s `customerId` prior to
 * Phase D's ViewModel wiring — see [resolveCustomerProfileState]. No longer used by the screen
 * itself (superseded by observing [ManagerCustomerProfileViewModel]'s own, independently-tested
 * [UiState] contract); kept only so `ManagerCustomerProfileScreenStateTest.kt` (committed with the
 * original P0 fix, outside Phase D's authorized files) keeps compiling. `internal` (not `private`)
 * for that same test-visibility reason.
 */
internal sealed interface CustomerProfileLookupState {
    data object Loading : CustomerProfileLookupState
    data class Found(val customer: ManagerCustomer) : CustomerProfileLookupState
    data object NotFound : CustomerProfileLookupState
    data class Error(val message: String) : CustomerProfileLookupState
}

/**
 * Pure decision function from the original P0 Safety Fix — retained only for
 * `ManagerCustomerProfileScreenStateTest.kt`'s sake (see [CustomerProfileLookupState]'s doc
 * comment); [ManagerCustomerProfileViewModel.load] is the real, live equivalent this screen now
 * actually uses.
 */
internal fun resolveCustomerProfileState(
    loadDetailResult: Result<Unit>,
    resolvedCustomer: ManagerCustomer?,
): CustomerProfileLookupState = loadDetailResult.fold(
    onSuccess = {
        if (resolvedCustomer != null) {
            CustomerProfileLookupState.Found(resolvedCustomer)
        } else {
            CustomerProfileLookupState.NotFound
        }
    },
    onFailure = { error -> CustomerProfileLookupState.Error(userMessageFor(error)) },
)

/** [onEditClick] (Customer Edit Flow, Phase 9 Step 1) routes to [ManagerCustomerEditScreen]; the phone row (Customer Contact Action, Phase 9 Step 3) launches the dialer when tapped. Name/phone text/visit-count/[TagChip] display is otherwise unchanged. */
@Composable
private fun CustomerIdentityHeader(customer: ManagerCustomer, onEditClick: () -> Unit) {
    val context = LocalContext.current

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
                    modifier = Modifier
                        .padding(top = RojanDimens.SpaceXS)
                        .let { rowModifier ->
                            if (customer.phone.isNotBlank()) {
                                rowModifier.rojanPressable(
                                    onClick = {
                                        // ACTION_DIAL (not ACTION_CALL): opens the dialer
                                        // pre-filled, doesn't place the call itself - needs no
                                        // CALL_PHONE runtime permission. Same pattern as
                                        // SalonDetailsScreen's salon-phone row.
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                                        runCatching { context.startActivity(intent) }
                                    },
                                )
                            } else {
                                rowModifier
                            }
                        },
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

            // 5B-2B: the layout footprint stays the visible icon size
            // (RojanIconSize.Medium) so the Row's spacedBy spacing and the
            // name/tag column are unaffected; the tap / semantics target is
            // a real 48dp, overflowing symmetrically into the surrounding
            // Row spacing. Same technique as Sprint 5B-1's bottom bar.
            Box(
                modifier = Modifier.size(RojanIconSize.Medium.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .requiredSize(RojanDimens.MinTouchTarget)
                        .rojanPressable(onClick = onEditClick, role = Role.Button),
                    contentAlignment = Alignment.Center,
                ) {
                    RojanIconContainer(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "ویرایش مشتری",
                        size = RojanIconSize.Medium,
                        tint = ManagerColors.Turquoise,
                    )
                }
            }
        }
    }
}

/** Insight → Customer Profile Action, Phase 7 Step 7 — one card per real, already-computed [ManagerCrmInsight] belonging to this customer; see [AiInsightRow]. */
@Composable
private fun AiInsightSection(insights: List<ManagerCrmInsight>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        RtlSectionHeader(
            text = "پیشنهاد هوش مصنوعی",
            style = RojanTypography.SectionTitle,
            color = ManagerColors.TextPrimary,
            horizontalPadding = 0.dp,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            insights.forEach { insight -> AiInsightRow(insight) }
        }
    }
}

/** Shows [ManagerCrmInsight.title]/[ManagerCrmInsight.reason] as-is — the real text a provider already computed, nothing derived or added here. Same icon/accent as [ai.rojan.designlab.manager.components.AIInsightCard] for visual consistency across the two surfaces. */
@Composable
private fun AiInsightRow(insight: ManagerCrmInsight) {
    ManagerGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RojanShapes.GlassCard,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            ManagerIconContainer(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "پیشنهاد هوش مصنوعی",
                containerSize = 44.dp,
                accentColor = ManagerColors.Gold,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = insight.title, style = RojanTypography.Body, color = ManagerColors.TextPrimary)
                Text(
                    text = insight.reason,
                    style = RojanTypography.Caption,
                    color = ManagerColors.TextSecondary,
                    modifier = Modifier.padding(top = RojanDimens.SpaceXS),
                )
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

/**
 * CRM Foundation, Phase 6 Step 5 — every real manager note on this customer, newest first.
 *
 * Phase F4: gains [AddNoteForm] — the create side. [notes] itself is still purely a display list;
 * creating a note flows entirely through [onSubmitNote] ([ManagerCustomerProfileViewModel.submitNote]),
 * which re-fetches from the backend on success rather than this composable ever locally inserting
 * into [notes].
 */
@Composable
private fun ManagerNotesSection(
    notes: List<CustomerNote>,
    submissionState: NoteSubmissionState,
    onSubmitNote: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        RtlSectionHeader(
            text = "یادداشت‌های مدیر",
            style = RojanTypography.SectionTitle,
            color = ManagerColors.TextPrimary,
            horizontalPadding = 0.dp,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceMD),
        ) {
            AddNoteForm(submissionState = submissionState, onSubmit = onSubmitNote)

            if (notes.isEmpty()) {
                ManagerGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
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
                    verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
                ) {
                    notes.forEach { note -> ManagerNoteRow(note) }
                }
            }
        }
    }
}

/**
 * Phase F4 — the Notes section's Create CTA: an always-visible inline input + submit button (no new
 * dialog/sheet component; this codebase has no existing Manager dialog/sheet pattern to reuse, and
 * [ManagerCustomerEditScreen]'s inline-form shape is the closest existing pattern, reused here).
 *
 * Client-side blank/length gating mirrors the backend's own `@NotBlank`/`@Size(max = 2000)` - see
 * [ManagerCustomerProfileViewModel.submitNote]'s own doc comment; this is a UX shortcut only, the
 * backend still validates independently.
 */
@Composable
private fun AddNoteForm(submissionState: NoteSubmissionState, onSubmit: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val isSubmitting = submissionState is NoteSubmissionState.Submitting
    val isValid = text.isNotBlank() && text.length <= 2000

    // Clears the input only on a genuine Submitting -> Idle transition (a real success) - never on
    // Failed (the user's typed text must survive a failed submit so they can retry without
    // retyping), and never on first composition (both start Idle, so the guard never fires there).
    var previousSubmissionState by remember { mutableStateOf<NoteSubmissionState>(NoteSubmissionState.Idle) }
    LaunchedEffect(submissionState) {
        if (previousSubmissionState is NoteSubmissionState.Submitting && submissionState is NoteSubmissionState.Idle) {
            text = ""
        }
        previousSubmissionState = submissionState
    }

    ManagerGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RojanShapes.GlassCard) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RojanDimens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("افزودن یادداشت جدید") },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = ManagerColors.TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ManagerColors.TextPrimary,
                    unfocusedTextColor = ManagerColors.TextPrimary,
                    focusedBorderColor = ManagerColors.Turquoise,
                    unfocusedBorderColor = ManagerColors.TextSecondary,
                    focusedLabelColor = ManagerColors.Turquoise,
                    unfocusedLabelColor = ManagerColors.TextSecondary,
                    cursorColor = ManagerColors.Turquoise,
                ),
            )

            if (submissionState is NoteSubmissionState.Failed) {
                Text(text = submissionState.message, style = RojanTypography.Caption, color = RojanErrorText)
            }

            ManagerPrimaryButton(
                text = "ثبت یادداشت",
                onClick = { onSubmit(text) },
                enabled = isValid && !isSubmitting,
                loading = isSubmitting,
            )
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
