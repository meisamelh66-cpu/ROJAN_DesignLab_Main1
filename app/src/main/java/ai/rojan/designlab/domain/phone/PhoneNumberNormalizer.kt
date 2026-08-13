package ai.rojan.designlab.domain.phone

/**
 * Normalizes an Iranian mobile number to E.164 before it reaches the
 * backend OTP endpoints (`/auth/otp/request`, `/auth/otp/verify`), which
 * require E.164 (`+98912xxxxxxx`) and reject anything else with
 * `400 INVALID_PHONE_NUMBER` (`ROJAN_Backend`'s `PhoneNumber` domain value
 * class, E.164-validated at the API boundary — see
 * `ROJAN_Mobile_Auth_Architecture_v1.md` §1). Shared across Customer/
 * Manager/Reception's auth ViewModels — one normalizer, applied
 * consistently, per `ROJAN System2 Android Parallel Work` Phase A item 1.
 *
 * Handles exactly the two shapes specified — nothing guessed beyond them:
 * - Local format (`0912xxxxxxx`) -> `+98912xxxxxxx` (leading `0` replaced
 *   by `+98`).
 * - Already E.164 (`+98912xxxxxxx`) -> returned unchanged.
 *
 * Any other shape (no leading `0`, no `+98` — e.g. a bare `912xxxxxxx`, or
 * a non-Iranian number) is returned trimmed but otherwise unchanged rather
 * than guessed at further. The backend's own validation remains the real,
 * final authority for anything this doesn't recognize; this function only
 * removes the one, common friction point where a naturally-typed local
 * number would otherwise be rejected outright.
 */
fun normalizeIranianPhoneNumber(input: String): String {
    val trimmed = input.trim()
    return when {
        trimmed.startsWith("+98") -> trimmed
        trimmed.startsWith("0") -> "+98${trimmed.removePrefix("0")}"
        else -> trimmed
    }
}
