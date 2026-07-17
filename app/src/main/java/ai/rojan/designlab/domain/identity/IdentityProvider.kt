package ai.rojan.designlab.domain.identity

/**
 * The interface [IdentityEngine] depends on — nothing in Domain knows
 * or cares whether the implementation is
 * [ai.rojan.designlab.data.identity.DemoIdentityProvider] or a future
 * `BackendIdentityProvider`. This is the actual Dependency Inversion
 * boundary the whole Identity Foundation sprint exists to establish.
 *
 * Identity Foundation Refinement (Patch 2): no longer exposes
 * `currentPerson()` — "who is currently active" is [SessionProvider]'s
 * job exclusively, not this interface's. This one only resolves IDs
 * into records, it never decides which ID is "current."
 *
 * Booking Experience Refactor — Authentication (Mock): [personByPhone]
 * and [registerPerson] added. Real login needs to look up a person by
 * phone number, and first-time signup needs to add a new one — both
 * belong here (not duplicated into [SessionProvider] or a separate
 * layer) since this interface already owns "how persons are resolved
 * and stored."
 */
interface IdentityProvider {
    fun personById(personId: String): PersonIdentity?
    fun personByPhone(phoneNumber: String): PersonIdentity?
    fun allPersons(): List<PersonIdentity>

    /** Creates and stores a new person, returning the fully-formed record with its assigned ID. ID assignment is the provider's own responsibility (a future backend would assign it server-side too) — callers never construct a [PersonIdentity] with an ID themselves. */
    fun registerPerson(displayName: String, verifiedMobile: String): PersonIdentity

    fun salonById(salonId: String): SalonIdentity?
    fun allSalons(): List<SalonIdentity>

    fun organizationById(organizationId: String): OrganizationIdentity?

    fun rolesFor(personId: String, salonId: String): Set<PersonRole>
}
