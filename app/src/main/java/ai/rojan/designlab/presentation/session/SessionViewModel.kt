package ai.rojan.designlab.presentation.session

import ai.rojan.designlab.domain.identity.IdentityProvider
import ai.rojan.designlab.domain.identity.PersonRole
import ai.rojan.designlab.domain.identity.rolesForPersonAcrossAllSalons
import ai.rojan.designlab.domain.model.Role
import ai.rojan.designlab.domain.repository.AuthSessionRepository
import ai.rojan.designlab.domain.usecase.ObserveSelectedRoleUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Whether the persisted role/identity has finished loading from storage yet. */
sealed interface SessionRestoreState {
    /** Still reading from DataStore — the very first frame(s) after launch only. */
    data object Loading : SessionRestoreState

    /**
     * Finished reading. [role] is `null` for a first-time user, non-null
     * for a returning one (unchanged from before UX Refactor Phase 2).
     * [personId] (added in Phase 2) is non-null when a previously
     * phone/OTP-verified customer's identity was persisted — see
     * [ai.rojan.designlab.presentation.auth.AuthViewModel.restoreSession].
     * [personRoles] (added in Phase 3) is [personId]'s real staff/customer
     * roles, resolved at restore time — [ai.rojan.designlab.navigation.RojanNavGraph]'s
     * `startDestination` needs this synchronously, before `AuthViewModel`
     * itself gets a chance to hydrate (its restore effect runs after
     * `NavHost` first composes, too late for `startDestination` — see this
     * phase's plan doc for why the lookup has to happen here instead).
     */
    data class Restored(val role: Role?, val personId: String?, val personRoles: Set<PersonRole>) : SessionRestoreState
}

/**
 * Restores the previously selected [Role] (if any) once per app launch,
 * so a returning user is routed straight to their dashboard instead of
 * seeing the Welcome/role-selection screen again.
 *
 * Production fix (physical-device launch freeze): previously this used
 * `observeSelectedRole().stateIn(...)` directly — if the upstream Flow
 * ever failed *or* simply never emitted for some reason not covered by
 * [ai.rojan.designlab.data.repository.RoleRepositoryImpl]'s own recovery,
 * [restoreState] would stay at `Loading` forever, with nothing else in
 * the app able to notice or recover. This class now adds a second,
 * independent layer of protection on top of that repository-level fix:
 * a hard timeout. If no value has arrived within
 * [SESSION_RESTORE_TIMEOUT_MS], restoration fails safe to
 * `Restored(role = null, personId = null)` — the same state a genuine
 * first-time user produces — so [ai.rojan.designlab.navigation.RojanNavGraph]
 * proceeds to Member Salons List instead of freezing. This is
 * deliberately independent of the repository-level fix: either one alone
 * would have prevented the originally-diagnosed freeze, and keeping both
 * means a *different*, not-yet-seen failure mode still can't reproduce it.
 *
 * UX Refactor Phase 2: also restores the persisted logged-in customer
 * identity (via [authSessionRepository]) under this exact same
 * Loading/timeout-guard shape, rather than a second, separate restore
 * mechanism — this pattern is already proven necessary on real devices,
 * so the new identity restore gets the same protection, not a weaker one.
 *
 * UX Refactor Phase 3: [identityProvider] resolves [personId]'s roles at
 * the same restore point, so a returning staff member's cold start can
 * route straight to their dashboard too. A second, independently-
 * constructed [IdentityProvider] instance from [AuthViewModel]'s is
 * deliberately fine here rather than a shared singleton: the 7 demo seed
 * accounts' role assignments are hardcoded in the class initializer and
 * identical across any instance, and a same-run first-time signup (the
 * one case a second instance genuinely couldn't see) is correctly denied
 * business access regardless, since [ai.rojan.designlab.data.identity.DemoIdentityProvider.registerPerson]
 * only ever grants [PersonRole.CUSTOMER].
 */
class SessionViewModel(
    observeSelectedRole: ObserveSelectedRoleUseCase,
    authSessionRepository: AuthSessionRepository,
    private val identityProvider: IdentityProvider,
) : ViewModel() {

    private val _restoreState =
        MutableStateFlow<SessionRestoreState>(SessionRestoreState.Loading)

    val restoreState: StateFlow<SessionRestoreState> = _restoreState.asStateFlow()

    init {
        viewModelScope.launch {

            // Timeout guard: if the first combined value hasn't arrived by
            // the deadline, fail safe to "no role, no identity" rather than
            // staying on Loading forever. compareAndSet only applies this
            // fallback if restoreState is still genuinely Loading — if the
            // real value already arrived first, this is a harmless no-op.
            val timeoutGuard = launch {
                delay(SESSION_RESTORE_TIMEOUT_MS)
                _restoreState.compareAndSet(
                    SessionRestoreState.Loading,
                    SessionRestoreState.Restored(role = null, personId = null, personRoles = emptySet())
                )
            }

            combine(
                observeSelectedRole()
                    .catch { throwable ->
                        // Defense in depth: RoleRepositoryImpl already recovers
                        // from read failures on its own, but if some future
                        // change to that layer regresses this, session
                        // restoration still must not die here either.
                        // CancellationException is never swallowed.
                        if (throwable is CancellationException) throw throwable
                        emit(null)
                    },
                authSessionRepository.observePersonId()
                    .catch { throwable ->
                        // Same defense-in-depth reasoning as the role flow
                        // above — AuthSessionRepositoryImpl already recovers
                        // internally, this is a second, independent layer.
                        if (throwable is CancellationException) throw throwable
                        emit(null)
                    },
            ) { role, personId -> role to personId }
                .collect { (role, personId) ->
                    timeoutGuard.cancel()
                    val personRoles = personId
                        ?.let { identityProvider.rolesForPersonAcrossAllSalons(it) }
                        ?: emptySet()
                    _restoreState.value = SessionRestoreState.Restored(role, personId, personRoles)
                }
        }
    }

    private companion object {
        /**
         * Generous relative to a normal DataStore read (typically single
         * milliseconds), but short enough that a genuinely broken restore
         * doesn't leave a real user staring at a loading screen for long.
         */
        const val SESSION_RESTORE_TIMEOUT_MS = 5_000L
    }
}
