package ai.rojan.designlab.data.identity

import ai.rojan.designlab.domain.identity.IdentityIdFormat
import ai.rojan.designlab.domain.identity.IdentityProvider
import ai.rojan.designlab.domain.identity.OrganizationIdentity
import ai.rojan.designlab.domain.identity.PersonIdentity
import ai.rojan.designlab.domain.identity.PersonRole
import ai.rojan.designlab.domain.identity.PersonRoleAssignment
import ai.rojan.designlab.domain.identity.SalonIdentity

/**
 * Infrastructure implementation of [IdentityProvider] — Identity
 * Foundation sprint. Simulates a realistic future backend: one
 * organization operating two salons (genuinely exercising Multi-Branch,
 * not just claiming support for it), with persons covering every role
 * named in the RuleBook, including one person holding two roles at once
 * (Owner + Finance) so "a person may have multiple roles" is
 * structurally real here, not just modeled-but-unused.
 *
 * All IDs use [IdentityIdFormat] — the shared contract shape a future
 * `BackendIdentityProvider` would also produce. These are demo values,
 * not permanent backend-issued identities — the permanence, uniqueness,
 * and non-reuse guarantees the RuleBook describes are properties of a
 * real identity system, which doesn't exist yet; this class only
 * satisfies the *shape* of that contract so the rest of the
 * architecture can be built and later swapped over without changes.
 *
 * Identity & Session Architecture Cleanup: [persons] is back to an
 * immutable `listOf` — the mock first-time-signup path that used to grow
 * it at runtime (`registerPerson`) is removed, confirmed zero real
 * callers remained once real backend OTP replaced the mock login flow.
 */
class DemoIdentityProvider : IdentityProvider {

    private val organization = OrganizationIdentity(
        id = "RJ-ORG-000001",
        name = "گروه زیبایی رویا",
    )

    private val salons = listOf(
        SalonIdentity(IdentityIdFormat.salonId(1), "سالن رویا", organization.id),
        SalonIdentity(IdentityIdFormat.salonId(2), "استودیو luxe", organization.id),
    )

    private val persons = listOf(
        PersonIdentity(IdentityIdFormat.personId(1), "رضا کریمی", "09120000001"),
        PersonIdentity(IdentityIdFormat.personId(2), "مریم صادقی", "09120000002"),
        PersonIdentity(IdentityIdFormat.personId(3), "سارا نجفی", "09120000003"),
        PersonIdentity(IdentityIdFormat.personId(4), "علی محمدی", "09120000004"),
        PersonIdentity(IdentityIdFormat.personId(5), "سارا احمدی", "09120000005"),
        PersonIdentity(IdentityIdFormat.personId(6), "رها احمدی", "09120000006"),
        PersonIdentity(IdentityIdFormat.personId(7), "نگین رضایی", "09120000007"),
    )

    private val roleAssignments = mutableListOf(
        // Owner + Finance at once - the multi-role case, genuinely exercised.
        PersonRoleAssignment(persons[0].id, salons[0].id, PersonRole.OWNER),
        PersonRoleAssignment(persons[0].id, salons[0].id, PersonRole.FINANCE),
        PersonRoleAssignment(persons[1].id, salons[0].id, PersonRole.GENERAL_MANAGER),
        PersonRoleAssignment(persons[2].id, salons[0].id, PersonRole.RECEPTION),
        PersonRoleAssignment(persons[3].id, salons[0].id, PersonRole.HR),
        PersonRoleAssignment(persons[4].id, salons[0].id, PersonRole.SPECIALIST),
        PersonRoleAssignment(persons[5].id, salons[0].id, PersonRole.CUSTOMER),
        // Second salon, different manager - genuine multi-branch structure.
        PersonRoleAssignment(persons[6].id, salons[1].id, PersonRole.GENERAL_MANAGER),
    )

    override fun personById(personId: String): PersonIdentity? = persons.find { it.id == personId }

    override fun salonById(salonId: String): SalonIdentity? = salons.find { it.id == salonId }

    override fun allSalons(): List<SalonIdentity> = salons

    override fun organizationById(organizationId: String): OrganizationIdentity? =
        if (organizationId == organization.id) organization else null

    override fun rolesFor(personId: String, salonId: String): Set<PersonRole> =
        roleAssignments
            .filter { it.personId == personId && it.salonId == salonId }
            .map { it.role }
            .toSet()
}
