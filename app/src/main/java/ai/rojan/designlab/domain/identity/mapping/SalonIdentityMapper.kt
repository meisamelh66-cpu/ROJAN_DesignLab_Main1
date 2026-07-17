package ai.rojan.designlab.domain.identity.mapping

import ai.rojan.designlab.data.demo.DemoSalon
import ai.rojan.designlab.domain.identity.SalonIdentity

/**
 * Identity Foundation Refinement (Patch 3): a mapping contract only —
 * no implementation, per explicit instruction. The Booking domain's
 * [DemoSalon] (a salon *listing* — rating, address, facilities, for
 * browsing/booking) and the Identity domain's [SalonIdentity] (a salon
 * *tenant* — permanent, owns customers/staff/inventory/finance) are
 * deliberately not merged and not duplicated into one model; this
 * interface is the seam between them, used only where a caller
 * genuinely needs to cross from one domain into the other.
 *
 * No implementation exists yet because there is no real relationship
 * data yet connecting a `DemoSalon` to a specific `SalonIdentity` (they
 * currently reference unrelated demo datasets, as flagged as a risk in
 * the original Identity Foundation report) — implementing this
 * correctly means resolving that relationship first, not guessing at
 * one now.
 */
interface SalonIdentityMapper {
    fun toSalonIdentity(demoSalon: DemoSalon): SalonIdentity?
}
