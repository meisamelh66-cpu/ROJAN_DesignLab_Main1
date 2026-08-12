package ai.rojan.designlab.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Active Salon Context & Selection Flow: which relation type an
 * [AvailableSalon]/[ActiveSalonContext] came from within
 * [CurrentUserIdentityContext] - carried through so the UI can label a
 * salon (owner/member/specialist) without re-deriving it.
 */
enum class SalonAccessType {
    OWNER,
    MEMBER,
    SPECIALIST,
}

/** One salon the current user could set as active - a candidate offered by [availableSalons], not yet a selection. */
data class AvailableSalon(
    val salonId: String,
    val salonName: String,
    val accessType: SalonAccessType,
    val permissions: Set<String>,
)

/** The salon the user has actually selected (or was auto-selected for them, when only one was available). */
data class ActiveSalonContext(
    val salonId: String,
    val salonName: String,
    val accessType: SalonAccessType,
    val permissions: Set<String>,
)

/**
 * Derives every salon the current user could set as active from
 * [CurrentUserIdentityContext.ownedSalons]/[CurrentUserIdentityContext.memberships]/
 * [CurrentUserIdentityContext.specialistLinks] - pure, no I/O, so callers
 * decide selection (auto vs. prompt) rather than this function guessing.
 *
 * Inactive entries (`active == false`) are excluded - they're a real,
 * revoked/disabled relation, not a selectable salon. A salon reachable via
 * more than one relation (e.g. both owned and a membership row exists for
 * it) is deduped by [AvailableSalon.salonId] rather than listed twice, kept
 * once under the highest-access relation - Owner, then Member, then
 * Specialist - since that's the relation that actually describes this
 * user's real access to it.
 */
fun CurrentUserIdentityContext.availableSalons(): List<AvailableSalon> {
    val bySalonId = LinkedHashMap<String, AvailableSalon>()

    ownedSalons.filter { it.active }.forEach { owned ->
        bySalonId[owned.salonId] = AvailableSalon(
            salonId = owned.salonId,
            salonName = owned.salonName,
            accessType = SalonAccessType.OWNER,
            permissions = owned.permissions,
        )
    }
    memberships.filter { it.active }.forEach { membership ->
        bySalonId.putIfAbsent(
            membership.salonId,
            AvailableSalon(
                salonId = membership.salonId,
                salonName = membership.salonName,
                accessType = SalonAccessType.MEMBER,
                permissions = membership.permissions,
            ),
        )
    }
    specialistLinks.filter { it.active }.forEach { specialist ->
        bySalonId.putIfAbsent(
            specialist.salonId,
            AvailableSalon(
                salonId = specialist.salonId,
                salonName = specialist.salonName,
                accessType = SalonAccessType.SPECIALIST,
                permissions = specialist.permissions,
            ),
        )
    }

    return bySalonId.values.toList()
}

/** Resolves this candidate as the actual [ActiveSalonContext] selection. */
fun AvailableSalon.toActiveSalonContext() = ActiveSalonContext(
    salonId = salonId,
    salonName = salonName,
    accessType = accessType,
    permissions = permissions,
)

/**
 * Persists which salon id the current user has set as active, and exposes
 * it back - same shape as [AuthSessionRepository] (persist the durable id
 * only, never the richer object, so nothing here can go stale relative to a
 * freshly-fetched [CurrentUserIdentityContext]).
 */
interface ActiveSalonContextRepository {

    /** Persists [salonId] as the active salon, replacing any previous value. */
    suspend fun saveActiveSalonId(salonId: String)

    /** Clears the persisted active salon id (e.g. on logout). */
    suspend fun clearActiveSalonId()

    /** Emits the currently persisted active salon id (`null` if none selected yet), and re-emits whenever it changes. */
    fun observeActiveSalonId(): Flow<String?>
}
