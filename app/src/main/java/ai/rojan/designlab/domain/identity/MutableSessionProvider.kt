package ai.rojan.designlab.domain.identity

/**
 * Booking Experience Refactor addition: a non-breaking EXTENSION of the
 * sealed [SessionProvider] interface — not a modification of it.
 * [SessionProvider] itself is untouched (per its seal: "may only be
 * modified if... a future Backend integration explicitly requires a
 * compatible Infrastructure adapter" — this is exactly that case,
 * handled additively rather than by editing the sealed file).
 *
 * The Identity Foundation's original [SessionProvider] assumed a
 * single, fixed, always-logged-in demo persona (an Owner). Real
 * authentication needs a session that starts logged OUT and becomes set
 * only after a real (or mock) login succeeds — this interface adds that
 * capability without changing what [SessionProvider] promises to
 * existing callers.
 */
interface MutableSessionProvider : SessionProvider {
    fun setSession(personId: String, salonId: String?, organizationId: String?)
    fun clearSession()
}
