package ai.rojan.designlab.manager.domain.repository

import ai.rojan.designlab.manager.domain.dashboard.ManagerSalonSummary

/**
 * Owner-scoped Salon Identity read/write (Phase A). Talks to the real
 * `POST/PUT /api/v1/salons` + `GET /api/v1/salons/mine` endpoints — all
 * three already exist and work on the backend today (verified directly
 * against `ROJAN_Backend` source). Reuses [ManagerSalonSummary] (the
 * same type [ai.rojan.designlab.manager.components.SalonIdentityCard]
 * already renders) rather than introducing a second, overlapping
 * salon-identity domain type.
 *
 * [createSalon] is scoped to `name`/`description`/`phone`/`email`/`address`
 * only, matching the backend's `CreateSalonRequest` exactly — that
 * request has no `logoUrl`/`latitude`/`longitude`/`city` fields, full
 * stop, not a readiness gap. [updateSalon] additionally covers
 * [latitude]/[longitude] (Phase A Correction) — real, live fields on
 * `Salon.updateProfile()` with "null means leave unchanged" merge
 * semantics, same as the backend command they map to. `logoUrl`/`city`
 * remain out of scope for both: `logoUrl` has a backend field but no
 * upload endpoint to produce a real URL for it yet, and `city` has no
 * backend field at all.
 *
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

    /** [latitude]/[longitude]: `null` leaves the salon's existing coordinates unchanged, it does not clear them (backend merge semantics). */
    suspend fun updateSalon(
        salonId: String,
        name: String,
        description: String?,
        phone: String,
        email: String?,
        address: String,
        latitude: Double?,
        longitude: Double?,
    ): Result<ManagerSalonSummary>
}
