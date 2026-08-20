package ai.rojan.designlab.domain.identity

import kotlinx.coroutines.flow.SharedFlow

/**
 * ROJAN AI Customer Journey Phase 2 (P1-6): bridges a session-ending event
 * detected in the data layer (`TokenAuthenticator`'s refresh-token failure,
 * which already clears [ai.rojan.designlab.domain.repository.TokenRepository]/
 * [ai.rojan.designlab.domain.repository.AuthSessionRepository] itself) to the
 * presentation layer ([ai.rojan.designlab.presentation.auth.AuthViewModel]),
 * without the data layer depending on presentation-layer classes — the
 * interface lives in the domain layer for that reason, same pattern as
 * every other repository port in this package.
 *
 * Fire-and-forget by design: [notifyInvalidated] is called from
 * `Authenticator.authenticate`, a synchronous OkHttp SPI callback with no
 * coroutine scope of its own, so it cannot suspend to guarantee delivery —
 * an implementation backed by a buffered [kotlinx.coroutines.flow.MutableSharedFlow]
 * is expected to make dropping this specific event vanishingly unlikely in
 * practice (a subscriber is already alive by the time a real session can
 * exist to invalidate), not to give a hard delivery guarantee.
 */
interface SessionInvalidationNotifier {
    val invalidations: SharedFlow<Unit>
    fun notifyInvalidated()
}
