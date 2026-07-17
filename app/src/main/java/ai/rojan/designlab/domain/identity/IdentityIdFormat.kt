package ai.rojan.designlab.domain.identity

/**
 * The approved RuleBook ID contract formats (`RJ-SALON-000001`,
 * `RJ-PERSON-000001`) — lives in Domain so both
 * [ai.rojan.designlab.data.identity.DemoIdentityProvider] and any future
 * `BackendIdentityProvider` format IDs identically. Neither this format
 * nor this class knows anything about where an ID's underlying numeric
 * sequence comes from (Demo: a local counter; Backend: whatever the
 * server issues) — it only defines the shared *shape* both must produce.
 */
object IdentityIdFormat {
    fun salonId(sequence: Int): String = "RJ-SALON-%06d".format(sequence)
    fun personId(sequence: Int): String = "RJ-PERSON-%06d".format(sequence)
}
