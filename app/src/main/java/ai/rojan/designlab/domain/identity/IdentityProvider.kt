package ai.rojan.designlab.domain.identity

/**
 * Nothing above this interface knows or cares whether the implementation
 * is [ai.rojan.designlab.data.identity.DemoIdentityProvider] or a future
 * `BackendIdentityProvider`. This is the Dependency Inversion boundary the
 * Identity Foundation exists to establish — it only resolves IDs into
 * records, it never decides which ID is "current" ([SessionProvider]'s
 * job exclusively).
 *
 * Identity & Session Architecture Cleanup: [personByPhone]/[registerPerson]/
 * `allPersons()` (added for the mock phone/OTP login this interface used
 * to back directly) are removed — confirmed zero real callers remained
 * once real backend OTP replaced the mock flow. [personById] stays: it's
 * still the live lookup behind `AuthViewModel.currentDisplayName` for a
 * real, backend-authenticated user id.
 */
interface IdentityProvider {
    fun personById(personId: String): PersonIdentity?

    fun salonById(salonId: String): SalonIdentity?
    fun allSalons(): List<SalonIdentity>

    fun organizationById(organizationId: String): OrganizationIdentity?

    fun rolesFor(personId: String, salonId: String): Set<PersonRole>
}

/**
 * UX Refactor Phase 3: every [PersonRole] [personId] holds across every
 * salon, not just one. [rolesFor] is scoped per-salon by design (a person
 * may hold different roles at different salons — see [PersonRoleAssignment]) —
 * but deciding whether someone has *any* staff access at all (for the
 * business-login flow, and for [ai.rojan.designlab.presentation.session.SessionViewModel]'s
 * cold-start restore) needs the union across every salon they're
 * assigned to, since [ai.rojan.designlab.domain.identity.SessionProvider.currentSalonId]
 * is a fixed reference value that doesn't reflect which salon a given
 * person actually belongs to.
 */
fun IdentityProvider.rolesForPersonAcrossAllSalons(personId: String): Set<PersonRole> =
    allSalons()
        .flatMap { salon -> rolesFor(personId, salon.id) }
        .toSet()
