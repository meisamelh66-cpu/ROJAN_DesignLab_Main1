package ai.rojan.designlab.reception.navigation

import ai.rojan.designlab.domain.repository.ActiveSalonContext
import ai.rojan.designlab.domain.repository.SalonAccessType
import ai.rojan.designlab.reception.domain.auth.ActiveSalonUiState
import ai.rojan.designlab.reception.domain.auth.ReceptionAuthState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Sprint 5B-7 (5B7-1) — [receptionSalonRecovery] is the pure decision
 * behind `WithActiveSalon`. After process death, Navigation restores a
 * salon-scoped screen (booking step / dashboard / customers) before the
 * active salon has necessarily re-resolved; this used to hit a hard
 * `check()` and crash. These lock in the recovery routing.
 */
class ReceptionSalonRecoveryTest {

    private val authed = ReceptionAuthState.Authenticated(userId = "u1", fullName = "Reception User")

    private fun active(salonId: String = "salon-1") = ActiveSalonUiState.Active(
        ActiveSalonContext(
            salonId = salonId,
            salonName = "Salon",
            accessType = SalonAccessType.MEMBER,
            permissions = emptySet(),
        ),
    )

    @Test
    fun `active salon renders the screen with the resolved id — behaviour unchanged`() {
        assertEquals(
            ReceptionSalonGate.Ready("salon-42"),
            receptionSalonRecovery(authed, active("salon-42")),
        )
    }

    @Test
    fun `active salon renders even if the auth flow re-reports Checking`() {
        // Restore order can surface the resolved salon before authState settles.
        assertEquals(
            ReceptionSalonGate.Ready("salon-1"),
            receptionSalonRecovery(ReceptionAuthState.Checking, active()),
        )
    }

    @Test
    fun `authenticated + salon-access error recovers to the access-error screen`() {
        assertEquals(
            ReceptionSalonGate.Recover(ReceptionDestinations.ACCESS_ERROR),
            receptionSalonRecovery(authed, ActiveSalonUiState.Error("salon access failed")),
        )
    }

    @Test
    fun `authenticated + selection required recovers to salon selection`() {
        assertEquals(
            ReceptionSalonGate.Recover(ReceptionDestinations.SALON_SELECTION),
            receptionSalonRecovery(authed, ActiveSalonUiState.SelectionRequired(emptyList())),
        )
    }

    @Test
    fun `authenticated + still resolving waits rather than navigating`() {
        assertEquals(
            ReceptionSalonGate.Wait,
            receptionSalonRecovery(authed, ActiveSalonUiState.Loading),
        )
    }

    @Test
    fun `no session behind the restored screen recovers to sign-in`() {
        // logout / expired refresh token leaves activeSalonState == Loading forever
        assertEquals(
            ReceptionSalonGate.Recover(ReceptionDestinations.OTP_AUTH),
            receptionSalonRecovery(ReceptionAuthState.Unauthenticated, ActiveSalonUiState.Loading),
        )
        assertEquals(
            ReceptionSalonGate.Recover(ReceptionDestinations.OTP_AUTH),
            receptionSalonRecovery(ReceptionAuthState.Checking, ActiveSalonUiState.Error("x")),
        )
    }
}
