package ai.rojan.designlab.manager.domain.repository

import ai.rojan.designlab.manager.domain.dashboard.ManagerSalonSummary

/**
 * Owner-scoped Salon Identity read/write (Phase A). Talks to the real
 * `POST/PUT /api/v1/salons` + `GET /api/v1/salons/mine` endpoints — all
 * three already exist and work on the backend today, per
 * `ROJAN_PhaseA_Salon_Identity_Readiness_Report_v1.md` §1-2. Reuses
 * [ManagerSalonSummary] (the same type
 * [ai.rojan.designlab.manager.components.SalonIdentityCard] already
 * renders) rather than introducing a second, overlapping salon-identity
 * domain type.
 *
 * Deliberately scoped to `name`/`description`/`phone`/`email`/`address`
 * only — `city`, logo, and cover image have no backend field to write to
 * yet (readiness report §7-8) and are not part of this contract.
 * [getMySalon] assumes at most one owned salon (`.firstOrNull()` on the
 * backend's `mine` list) - a disclosed simplification matching this
 * phase's single-salon-pilot scope, not a silent one; the backend itself
 * supports an owner having more than one.
 */
interface ManagerSalonRepository {

    /** `null` means the owner has no salon yet (create mode); a real result means edit mode. */
    suspend fun getMySalon(): Result<ManagerSalonSummary?>

    suspend fun createSalon(
        name: String,
        description: String?,
        phone: String,
        email: String?,
        address: String,
    ): Result<ManagerSalonSummary>

    suspend fun updateSalon(
        salonId: String,
        name: String,
        description: String?,
        phone: String,
        email: String?,
        address: String,
    ): Result<ManagerSalonSummary>
}
