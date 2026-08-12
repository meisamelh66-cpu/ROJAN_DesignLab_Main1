package ai.rojan.designlab.domain.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Active Salon Context & Selection Flow — pure, hermetic coverage of
 * [CurrentUserIdentityContext.availableSalons], the function that decides
 * which salons are even offered as candidates (auto-select/prompt decisions
 * themselves live in [ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModel],
 * covered separately).
 */
class ActiveSalonContextTest {

    private val identityContext = CurrentUserIdentityContext(
        userId = "user-1",
        phoneNumber = "+989123456789",
        email = null,
        fullName = "Sara",
        globalRole = "MANAGER",
        ownedSalons = emptyList(),
        memberships = emptyList(),
        specialistLinks = emptyList(),
    )

    @Test
    fun `no relations at all yields no available salons`() {
        assertEquals(emptyList<AvailableSalon>(), identityContext.availableSalons())
    }

    @Test
    fun `an inactive relation is excluded`() {
        val context = identityContext.copy(
            ownedSalons = listOf(OwnedSalonAccess(salonId = "s1", salonName = "Salon One", active = false, permissions = emptySet())),
        )

        assertEquals(emptyList<AvailableSalon>(), context.availableSalons())
    }

    @Test
    fun `active relations across all three sources are all offered`() {
        val context = identityContext.copy(
            ownedSalons = listOf(OwnedSalonAccess(salonId = "s1", salonName = "Salon One", active = true, permissions = setOf("MANAGE_SALON"))),
            memberships = listOf(SalonMembershipAccess(membershipId = "m1", salonId = "s2", salonName = "Salon Two", active = true, role = "STAFF", permissions = emptySet())),
            specialistLinks = listOf(SpecialistAccess(specialistId = "sp1", salonId = "s3", salonName = "Salon Three", active = true, permissions = emptySet())),
        )

        val available = context.availableSalons()

        assertEquals(3, available.size)
        assertEquals(setOf("s1", "s2", "s3"), available.map { it.salonId }.toSet())
        assertEquals(SalonAccessType.OWNER, available.first { it.salonId == "s1" }.accessType)
        assertEquals(SalonAccessType.MEMBER, available.first { it.salonId == "s2" }.accessType)
        assertEquals(SalonAccessType.SPECIALIST, available.first { it.salonId == "s3" }.accessType)
    }

    @Test
    fun `the same salon reachable via more than one relation is deduped, keeping the highest-access relation`() {
        val context = identityContext.copy(
            ownedSalons = listOf(OwnedSalonAccess(salonId = "s1", salonName = "Salon One", active = true, permissions = setOf("MANAGE_SALON"))),
            memberships = listOf(SalonMembershipAccess(membershipId = "m1", salonId = "s1", salonName = "Salon One", active = true, role = "STAFF", permissions = emptySet())),
            specialistLinks = listOf(SpecialistAccess(specialistId = "sp1", salonId = "s1", salonName = "Salon One", active = true, permissions = emptySet())),
        )

        val available = context.availableSalons()

        assertEquals(1, available.size)
        assertEquals(SalonAccessType.OWNER, available.single().accessType)
    }

    @Test
    fun `an inactive owned salon does not shadow an active membership for the same id`() {
        val context = identityContext.copy(
            ownedSalons = listOf(OwnedSalonAccess(salonId = "s1", salonName = "Salon One", active = false, permissions = emptySet())),
            memberships = listOf(SalonMembershipAccess(membershipId = "m1", salonId = "s1", salonName = "Salon One", active = true, role = "STAFF", permissions = emptySet())),
        )

        val available = context.availableSalons()

        assertTrue(available.single().accessType == SalonAccessType.MEMBER)
    }
}
