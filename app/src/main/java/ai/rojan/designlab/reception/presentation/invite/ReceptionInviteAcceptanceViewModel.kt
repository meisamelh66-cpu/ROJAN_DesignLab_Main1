package ai.rojan.designlab.reception.presentation.invite

import ai.rojan.designlab.domain.phone.normalizeIranianPhoneNumber
import ai.rojan.designlab.domain.repository.AuthenticatedUser
import ai.rojan.designlab.domain.repository.CurrentUserIdentityContext
import ai.rojan.designlab.domain.repository.OtpIssued
import ai.rojan.designlab.domain.repository.availableSalons
import ai.rojan.designlab.presentation.common.userMessageFor
import ai.rojan.designlab.reception.domain.repository.SalonInviteAcceptance
import ai.rojan.designlab.reception.domain.repository.SalonInvitePreview
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Loading/Error/Preview/Accepted lifecycle for [ReceptionInviteAcceptanceViewModel] — same Loading/Error/Retry shape as P0-1/P0-2 ([ai.rojan.designlab.manager.presentation.dashboard.ManagerDashboardUiState]-style), plus the two states specific to an invite: the confirmation [Preview] (which also carries the OTP sub-flow) and the terminal [Accepted] result. */
sealed interface ReceptionInviteAcceptanceUiState {
    data object Loading : ReceptionInviteAcceptanceUiState

    /** Invite fetch failed — expired/revoked/already-accepted/never-existed are all indistinguishable here by backend design (see [ai.rojan.designlab.reception.domain.repository.SalonInvitePreview]'s own doc comment), so [message] is always the same generic "not found" copy. */
    data class Error(val message: String) : ReceptionInviteAcceptanceUiState

    /** A real, still-open invite — the confirmation + OTP screen. [salonName]/[role] never change again once loaded; the OTP sub-flow's own progress lives in [otpStep]/[isSubmitting]/[authErrorMessage] below, not in this state. */
    data class Preview(val salonName: String, val role: String) : ReceptionInviteAcceptanceUiState

    /** Acceptance + identity refresh + salon resolution all succeeded — [salonId] is real and ready to hand off to [ai.rojan.designlab.reception.navigation.ReceptionAcceptedSessionGraph]. */
    data class Accepted(
        val salonId: String,
        val salonName: String,
        val fullName: String,
        val phoneNumber: String?,
    ) : ReceptionInviteAcceptanceUiState
}

/** The [ReceptionInviteAcceptanceUiState.Preview] screen's own OTP step machine — same shape as [ai.rojan.designlab.reception.domain.auth.ReceptionOtpStep], kept as its own type rather than reused because this flow never touches [ai.rojan.designlab.reception.presentation.auth.ReceptionAuthViewModel] at all (see this file's own doc comment). */
sealed interface ReceptionInviteOtpStep {
    data object EnteringPhone : ReceptionInviteOtpStep
    data class AwaitingCode(val phoneNumber: String, val canResendAfterSeconds: Long) : ReceptionInviteOtpStep
}

/**
 * P1-1, Reception Invite Acceptance Flow: owns the entire "scan a QR code,
 * confirm the salon, verify OTP, accept, land on a real Dashboard" journey
 * for a newly-invited receptionist/manager — reached from
 * [ai.rojan.designlab.reception.navigation.ReceptionRootGraph] *before* (outside)
 * the existing [ai.rojan.designlab.reception.presentation.auth.ReceptionAuthViewModel]-gated
 * app, per this sprint's approved architecture decision.
 *
 * **Deliberately never constructs or touches [ai.rojan.designlab.reception.presentation.auth.ReceptionAuthViewModel]:**
 * that ViewModel's `restoreSession()`/`onVerified()` both reject (and
 * `clearSession()`) any account whose global `UserRole` isn't `MANAGER`
 * (`RECEPTION_GATE_ROLE`) — but a brand-new invitee's first-ever OTP
 * verification always auto-registers as `UserRole.CUSTOMER` on the backend
 * (`VerifyOtpUseCase`), and even an *existing* customer account keeps that
 * role forever (global `UserRole` is confirmed non-load-bearing everywhere
 * in the backend — real authorization is per-salon `SalonMembership.role`,
 * which is exactly what accepting this invite grants). Routing this flow
 * through the gated ViewModel would make the invite unreachable for the
 * exact accounts it exists to onboard. This ViewModel performs its own,
 * independent OTP verification directly against [ai.rojan.designlab.domain.repository.BackendAuthRepository] instead.
 *
 * Every backend call is injected as a suspend lambda (constructor
 * parameters below), not a repository singleton — same testability
 * convention P0-1/P0-2/P0-3 established, so [ReceptionInviteAcceptanceViewModelTest]
 * can assert genuine intermediate state via `CompletableDeferred` without a
 * hand-written fake repository class.
 *
 * On a successful [verifyOtpCode] the flow does not stop at "logged in" —
 * [completeAcceptance] runs [acceptInvite], then [getIdentityContext] (a
 * fresh `/users/me/salon-access` call — the real, authoritative source for
 * the accepted salon's name/permissions, not the accept response's own
 * minimal `salonId`+`role`), resolves the accepted salon from that fresh
 * context, persists it via [saveActiveSalonId], and only then reaches
 * [ReceptionInviteAcceptanceUiState.Accepted] — "refresh identity → fetch
 * fresh salon-access → resolve the accepted salon → route to Dashboard",
 * exactly the sequence this sprint's instructions required. The invite
 * [token] itself is held only in this constructor's closure — never passed
 * to [savePersonId]/[saveActiveSalonId] or any persistence call, and never
 * logged.
 */
class ReceptionInviteAcceptanceViewModel(
    private val token: String,
    private val getInvite: suspend (String) -> Result<SalonInvitePreview>,
    private val requestOtp: suspend (String) -> Result<OtpIssued>,
    private val verifyOtp: suspend (phoneNumber: String, code: String) -> Result<AuthenticatedUser>,
    private val acceptInvite: suspend (String) -> Result<SalonInviteAcceptance>,
    private val getIdentityContext: suspend () -> Result<CurrentUserIdentityContext>,
    private val saveActiveSalonId: suspend (String) -> Unit,
    private val savePersonId: suspend (String) -> Unit,
) : ViewModel() {

    var state by mutableStateOf<ReceptionInviteAcceptanceUiState>(ReceptionInviteAcceptanceUiState.Loading)
        private set

    private val _otpStep = MutableStateFlow<ReceptionInviteOtpStep>(ReceptionInviteOtpStep.EnteringPhone)
    val otpStep: StateFlow<ReceptionInviteOtpStep> = _otpStep.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    init {
        loadInvite()
    }

    private fun loadInvite() {
        state = ReceptionInviteAcceptanceUiState.Loading
        viewModelScope.launch {
            getInvite(token)
                .onSuccess { preview -> state = ReceptionInviteAcceptanceUiState.Preview(preview.salonName, preview.role) }
                .onFailure { error -> state = ReceptionInviteAcceptanceUiState.Error(userMessageFor(error)) }
        }
    }

    /** Re-fetches the invite after a load failure — mirrors P0-1/P0-2's `retry()` guard: a no-op while already loading. */
    fun retry() {
        if (state is ReceptionInviteAcceptanceUiState.Loading) return
        loadInvite()
    }

    /** `POST /api/v1/auth/otp/request` — [phoneNumber] is normalized to E.164 first, same reasoning as [ai.rojan.designlab.reception.presentation.auth.ReceptionAuthViewModel.requestOtp]. */
    fun requestOtpCode(phoneNumber: String) {
        val trimmed = normalizeIranianPhoneNumber(phoneNumber)
        if (trimmed.isBlank()) {
            _authErrorMessage.value = "شماره موبایل را وارد کنید"
            return
        }

        _authErrorMessage.value = null
        _isSubmitting.value = true

        viewModelScope.launch {
            requestOtp(trimmed)
                .onSuccess { issued -> _otpStep.value = ReceptionInviteOtpStep.AwaitingCode(issued.phoneNumber, issued.canResendAfterSeconds) }
                .onFailure { error -> _authErrorMessage.value = userMessageFor(error) }
            _isSubmitting.value = false
        }
    }

    /** Requests a fresh code for the same phone number. */
    fun resendOtpCode() {
        val step = _otpStep.value as? ReceptionInviteOtpStep.AwaitingCode ?: return
        requestOtpCode(step.phoneNumber)
    }

    /** Returns to the phone-entry step. */
    fun editPhoneNumber() {
        _authErrorMessage.value = null
        _otpStep.value = ReceptionInviteOtpStep.EnteringPhone
    }

    /** `POST /api/v1/auth/otp/verify`, then [completeAcceptance] on success — [isSubmitting] stays `true` through the whole accept+refresh sequence, not just the OTP call itself, so the UI shows one continuous "working" state instead of a misleading brief gap between login and acceptance. */
    fun verifyOtpCode(code: String) {
        val step = _otpStep.value as? ReceptionInviteOtpStep.AwaitingCode ?: return
        val trimmedCode = code.trim()
        if (trimmedCode.isBlank()) {
            _authErrorMessage.value = "کد تایید را وارد کنید"
            return
        }

        _authErrorMessage.value = null
        _isSubmitting.value = true

        viewModelScope.launch {
            verifyOtp(step.phoneNumber, trimmedCode)
                .onSuccess { user -> completeAcceptance(user) }
                .onFailure { error ->
                    _authErrorMessage.value = userMessageFor(error)
                    _isSubmitting.value = false
                }
        }
    }

    // Set once acceptInvite(token) genuinely succeeds, cleared once Accepted
    // is reached. A [SalonInvite] token is single-use on the backend
    // (`SalonInviteRepository.acceptIfAvailable`) — if the identity-refresh/
    // salon-resolution steps below fail *after* a real accept (e.g. a
    // transient network blip in that ~1-2s window), re-verifying OTP and
    // retrying via [verifyOtpCode] must not call [acceptInvite] a second
    // time, or the now-already-consumed token would 404 and strand an
    // already-legitimately-onboarded receptionist with no way to reach
    // Dashboard from this screen. [completeAcceptance] checks this first and
    // skips straight to [finishAcceptance] when set.
    private var pendingAcceptance: SalonInviteAcceptance? = null

    private suspend fun completeAcceptance(user: AuthenticatedUser) {
        savePersonId(user.id)

        val acceptance = pendingAcceptance ?: acceptInvite(token).getOrElse { error ->
            _authErrorMessage.value = userMessageFor(error)
            _isSubmitting.value = false
            return
        }
        pendingAcceptance = acceptance

        finishAcceptance(user, acceptance)
    }

    private suspend fun finishAcceptance(user: AuthenticatedUser, acceptance: SalonInviteAcceptance) {
        getIdentityContext()
            .onSuccess { context ->
                val resolved = context.availableSalons().firstOrNull { it.salonId == acceptance.salonId }
                if (resolved == null) {
                    // Real, if unlikely, race: the accept call above
                    // succeeded but this fresh salon-access fetch doesn't
                    // yet reflect it. Surfaced as a real error rather than
                    // silently retrying forever — [pendingAcceptance] stays
                    // set, so a user retry re-runs only this method, never
                    // re-consumes the token.
                    _authErrorMessage.value = "پذیرش دعوت ثبت شد اما دسترسی به سالن هنوز قابل شناسایی نیست. لطفاً دوباره تلاش کنید."
                    _isSubmitting.value = false
                    return@onSuccess
                }

                saveActiveSalonId(resolved.salonId)
                pendingAcceptance = null
                _isSubmitting.value = false
                state = ReceptionInviteAcceptanceUiState.Accepted(
                    salonId = resolved.salonId,
                    salonName = resolved.salonName,
                    fullName = user.fullName,
                    phoneNumber = context.phoneNumber,
                )
            }
            .onFailure { error ->
                _authErrorMessage.value = userMessageFor(error)
                _isSubmitting.value = false
            }
    }
}
